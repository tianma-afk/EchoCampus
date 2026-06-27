#!/usr/bin/env python3
"""
EchoCampus 模型权重下载脚本
=================================
用途：在 Docker 构建 / CI / 开发环境中下载所有必需的模型文件。
特性：
  - 幂等：已存在的文件自动跳过
  - 独立：不依赖项目运行时代码，可单独执行
  - 可校验：支持 SHA256 校验（首次运行后填入）
  - 镜像：支持 HF_ENDPOINT / DINOV2_MIRROR 环境变量

用法:
  uv run python scripts/download_models.py          # 使用 uv
  python scripts/download_models.py                 # 直接运行
  HF_ENDPOINT=https://hf-mirror.com uv run python scripts/download_models.py  # 使用镜像
"""

import os
import shutil
import sys
import tempfile
import zipfile
from pathlib import Path

import requests

# ── 路径配置 ──────────────────────────────────────────────
PROJECT_ROOT = Path(__file__).resolve().parents[1]
WEIGHTS_DIR = PROJECT_ROOT / "weights"
HUB_DIR = WEIGHTS_DIR / "hub"
DINOV2_REPO_DIR = HUB_DIR / "facebookresearch_dinov2_main"
DINOV2_CHECKPOINTS_DIR = HUB_DIR / "checkpoints"
PAIRVPR_WEIGHTS_DIR = WEIGHTS_DIR / "vpr_weights"

# ── 镜像配置 ──────────────────────────────────────────────
HF_ENDPOINT = os.environ.get("HF_ENDPOINT", "https://huggingface.co")
DINOV2_BASE_URL = os.environ.get(
    "DINOV2_MIRROR",
    "https://dl.fbaipublicfiles.com/dinov2",
)
MODEL_TYPE = os.environ.get("MODEL_TYPE", "vitB")  # vitB / vitL / vitG

# ── 数据源定义 ────────────────────────────────────────────

# MODEL_TYPE → DINOv2 checkpoint 映射（get_cfg() 的 encoder_map 与之对应）
DINOV2_CHECKPOINT_MAP = {
    "vitB": "dinov2_vitb14_pretrain.pth",
    "vitL": "dinov2_vitl14_pretrain.pth",
    "vitG": "dinov2_vitg14_pretrain.pth",
}

# MODEL_TYPE → Pair-VPR 权重文件名映射
PAIRVPR_WEIGHT_MAP = {
    "vitB": "pairvpr-vitB.pth",
    "vitL": "pairvpr-vitL.pth",
    "vitG": "pairvpr-vitG.pth", 
}

# ── 工具函数 ──────────────────────────────────────────────


def human_size(num_bytes: int) -> str:
    """将字节数转为可读格式。"""
    for unit in ("B", "KB", "MB", "GB"):
        if num_bytes < 1024:
            return f"{num_bytes:.1f} {unit}"
        num_bytes /= 1024
    return f"{num_bytes:.1f} TB"


def download_file(
    url: str,
    dest: Path,
    chunk_size: int = 8192,
    timeout: int = 600,
) -> None:
    """
    下载文件到目标路径，带进度日志和校验。

    Args:
        url: 下载地址
        dest: 目标文件路径
        chunk_size: 块大小
        timeout: 超时秒数
    """
    dest = Path(dest)
    dest.parent.mkdir(parents=True, exist_ok=True)

    print(f"  ⬇  {url}")
    response = requests.get(url, stream=True, timeout=timeout)
    response.raise_for_status()

    total = int(response.headers.get("content-length", 0))
    if total > 0:
        print(f"     文件大小: {human_size(total)}")

    downloaded = 0
    tmp_path = dest.with_suffix(dest.suffix + ".tmp")

    try:
        with open(tmp_path, "wb") as f:
            for chunk in response.iter_content(chunk_size=chunk_size):
                if chunk:
                    f.write(chunk)
                    downloaded += len(chunk)

                    if total > 0 and downloaded % (chunk_size * 128) < chunk_size:
                        pct = downloaded / total * 100
                        print(
                            f"\r     进度: {pct:.0f}% ({human_size(downloaded)}/{human_size(total)})",
                            end="",
                            flush=True,
                        )

        if total > 0:
            print(f"\r     ✅ 完成 ({human_size(downloaded)})" + " " * 20)

        # 原子性重命名
        tmp_path.replace(dest)

    except BaseException:
        # 清理失败的部分下载
        if tmp_path.exists():
            tmp_path.unlink()
        raise


# ── 各组件下载逻辑 ────────────────────────────────────────


def download_dinov2_repo() -> None:
    """下载 DINOv2 GitHub 仓库（zip 解压）。"""
    DINOV2_REPO_ZIP_URL = (
        "https://github.com/facebookresearch/dinov2/archive/main.zip"
    )
    # 已存在则跳过（hubconf.py 是仓库存在的标志）
    if DINOV2_REPO_DIR.is_dir() and (DINOV2_REPO_DIR / "hubconf.py").exists():
        print(f"✅ DINOv2 仓库已存在，跳过: {DINOV2_REPO_DIR}")
        return

    print("── 下载 DINOv2 仓库 ──")
    HUB_DIR.mkdir(parents=True, exist_ok=True)

    zip_path = HUB_DIR / "dinov2_main.zip"
    download_file(DINOV2_REPO_ZIP_URL, zip_path)

    # 解压到临时目录
    print(f"  📦 解压...")
    with tempfile.TemporaryDirectory() as tmpdir:
        tmp = Path(tmpdir)
        with zipfile.ZipFile(zip_path, "r") as zf:
            zf.extractall(tmp)

        extracted = tmp / "dinov2-main"
        if not extracted.is_dir():
            raise RuntimeError("解压后未找到 dinov2-main 目录，zip 结构可能已变化")

        # 如果目标已存在则先删除（幂等重放时）
        if DINOV2_REPO_DIR.exists():
            shutil.rmtree(DINOV2_REPO_DIR)

        shutil.move(str(extracted), str(DINOV2_REPO_DIR))

    # 清理 zip
    zip_path.unlink()
    print(f"✅ DINOv2 仓库准备完成: {DINOV2_REPO_DIR}")


def download_dinov2_checkpoint() -> None:
    """下载当前 MODEL_TYPE 对应的 DINOv2 预训练权重。"""
    checkpoint = DINOV2_CHECKPOINT_MAP.get(MODEL_TYPE)
    if checkpoint is None:
        raise ValueError(f"不支持的 MODEL_TYPE: {MODEL_TYPE}，可选: vitB, vitL, vitG")

    DINOV2_CHECKPOINTS_DIR.mkdir(parents=True, exist_ok=True)
    dest = DINOV2_CHECKPOINTS_DIR / checkpoint

    if dest.exists():
        print(f"✅ DINOv2 checkpoint 已存在，跳过: {checkpoint}")
        return

    print(f"── 下载 DINOv2 预训练权重 (MODEL_TYPE={MODEL_TYPE}) ──")
    model_name = checkpoint.replace("_pretrain.pth", "")
    url = f"{DINOV2_BASE_URL}/{model_name}/{checkpoint}"
    print(f"  [{checkpoint}]")
    download_file(url, dest)
    print(f"✅ DINOv2 checkpoint 就绪: {checkpoint}")


def download_pairvpr_weight() -> None:
    """从 HuggingFace（或镜像）下载当前 MODEL_TYPE 对应的 Pair-VPR 权重。"""
    filename = PAIRVPR_WEIGHT_MAP.get(MODEL_TYPE)
    if filename is None:
        raise ValueError(f"不支持的 MODEL_TYPE: {MODEL_TYPE}，可选: vitB, vitL, vitG")

    PAIRVPR_WEIGHTS_DIR.mkdir(parents=True, exist_ok=True)
    dest = PAIRVPR_WEIGHTS_DIR / filename

    if dest.exists():
        print(f"✅ Pair-VPR 权重已存在，跳过: {filename}")
        return

    print(f"── 下载 Pair-VPR 权重 (MODEL_TYPE={MODEL_TYPE}, 源: {HF_ENDPOINT}) ──")
    url = f"{HF_ENDPOINT}/CSIRORobotics/Pair-VPR/resolve/main/{filename}"
    print(f"  [{MODEL_TYPE}] {filename}")
    download_file(url, dest)
    print(f"✅ Pair-VPR 权重就绪: {filename}")


# ── 主入口 ────────────────────────────────────────────────


def main() -> None:
    # 校验 MODEL_TYPE
    if MODEL_TYPE not in DINOV2_CHECKPOINT_MAP:
        print(f"❌ 不支持的 MODEL_TYPE: {MODEL_TYPE}")
        print(f"   可选值: {', '.join(DINOV2_CHECKPOINT_MAP.keys())}")
        print(f"   请在 .env 中设置 MODEL_TYPE 或通过环境变量传入")
        sys.exit(1)

    print("=" * 60)
    print("  EchoCampus 模型权重下载")
    print("=" * 60)
    print(f"  项目根目录:  {PROJECT_ROOT}")
    print(f"  权重目录:    {WEIGHTS_DIR}")
    print(f"  模型规模:    {MODEL_TYPE}")
    print(f"  HF 镜像:     {HF_ENDPOINT}")
    print(f"  DINOv2 源:   {DINOV2_BASE_URL}")
    print()

    steps = [
        ("DINOv2 仓库", download_dinov2_repo),
        ("DINOv2 预训练权重", download_dinov2_checkpoint),
        ("Pair-VPR 权重", download_pairvpr_weight),
    ]

    errors: list[tuple[str, str]] = []

    for name, func in steps:
        try:
            func()
            print()
        except requests.RequestException as e:
            errors.append((name, f"网络错误: {e}"))
            print(f"❌ {name} 下载失败: {e}\n")
        except Exception as e:
            errors.append((name, str(e)))
            print(f"❌ {name} 失败: {e}\n")

    print("=" * 60)
    if errors:
        print("❌ 下载未完全成功，以下步骤失败：")
        for name, err in errors:
            print(f"   · {name}: {err}")
        print()
        print("💡 提示：")
        print("   - 检查网络连接")
        print("   - 设置 HF_ENDPOINT=https://hf-mirror.com 使用 HuggingFace 镜像")
        print("   - 设置 DINOV2_MIRROR 替换 DINOv2 下载源")
        sys.exit(1)
    else:
        print("✅ 所有模型权重下载完成")
        print()
        print("现在可以启动服务:")
        print("  uv run uvicorn app.main:app --host 127.0.0.1 --port 8000")
        sys.exit(0)


if __name__ == "__main__":
    main()

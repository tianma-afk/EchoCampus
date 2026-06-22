# ============================================================
# EchoCampus 图像相似度算法后端 (backend_py)
# Pair-VPR + DINOv2 + Milvus 向量检索服务
# ============================================================

FROM python:3.12-slim

WORKDIR /app

# ---- 系统依赖 ----
# libgomp1: PyTorch CPU 推理所需
# git: DINOv2 torch.hub 首次加载时需要 clone facebookresearch/dinov2 仓库
# wget: 预下载权重文件
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgomp1 \
    git \
    wget \
    && rm -rf /var/lib/apt/lists/*

# ---- 安装 uv ----
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv

# ---- 依赖安装（利用 Docker 层缓存） ----
COPY pyproject.toml uv.lock ./
RUN uv lock && uv sync --extra cpu

# ---- 复制源码 ----
COPY app/ ./app/

# ---- 下载模型权重----
ENV HF_ENDPOINT=https://hf-mirror.com

# 注意：源码中的绝对导入 (from core.xxx) 依赖 CWD 为 backend_py/app/
# 所以先 cd 到 /app/app 再执行 Python
RUN mkdir -p /app/weights /app/data /app/logs && \
    cd /app/app && \
    uv run python -c "\
from services.pair_vpr import PairVPRExtractor; \
print('>> 加载 Pair-VPR + DINOv2 模型权重（首次将从 hf-mirror.com 下载）...'); \
extractor = PairVPRExtractor(model_type='vitB', use_fp16=True); \
print('>> 模型权重加载完成，已内置于镜像中'); \
"

EXPOSE 8000

# 源码中的绝对导入 (from core.xxx / from services.xxx) 依赖 CWD 为 backend_py/app/
WORKDIR /app/app
CMD ["uv", "run", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]

# ============================================================
# EchoCampus 图像相似度算法后端 (backend_py)
# 多阶段构建：构建阶段处理依赖+模型，运行阶段只含运行时产物
# ============================================================

# ── 构建阶段 ──────────────────────────────────────────
FROM python:3.12-slim AS builder

WORKDIR /app

# 系统依赖（PyTorch CPU 推理所需）
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv

# 依赖安装 → /app/.venv
COPY pyproject.toml uv.lock ./
RUN uv lock && uv sync --frozen --extra cpu --no-dev

# 复制源码 + 下载脚本
COPY app/ ./app/
COPY scripts/ ./scripts/

# 下载模型权重
ENV HF_ENDPOINT=https://hf-mirror.com
RUN /app/.venv/bin/python scripts/download_models.py

# ── 运行阶段 ──────────────────────────────────────────
FROM python:3.12-slim AS runtime

WORKDIR /app

# 运行时系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# 从构建阶段仅复制产物
COPY --from=builder /app/.venv    /app/.venv
COPY --from=builder /app/weights  /app/weights
COPY app/ ./app/

EXPOSE 8000

# 使用 venv 的 python
WORKDIR /app/app
CMD ["/app/.venv/bin/python", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]

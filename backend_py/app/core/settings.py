from pydantic_settings import BaseSettings, SettingsConfigDict
from pathlib import Path



class Settings(BaseSettings):
    MILVUS_URI: str = "http://localhost:19530"
    MODEL_TYPE: str = "vitB"  # vitB / vitL / vitG

    # MinIO — 对象存储（原始图片 + dense_features token）
    MINIO_ENDPOINT: str = "localhost:9000"
    MINIO_ACCESS_KEY: str = "echo_campus"
    MINIO_SECRET_KEY: str = "echo_campus"
    MINIO_BUCKET: str = "image"
    MINIO_SECURE: bool = False  # 生产环境启用 HTTPS 时设为 True
    MINIO_DENSE_FEATURES_PREFIX: str = "tokens"  # dense_features 存于 tokens/{uuid}.npy

    # Docker 网络内 MinIO 访问 — 将外部 URL 改写为容器内 hostname
    MINIO_INTERNAL_HOST: str = ""

    model_config = SettingsConfigDict(
        env_file=Path(__file__).parent.parent.parent / ".env",
        env_file_encoding="utf-8",
    )


settings = Settings()
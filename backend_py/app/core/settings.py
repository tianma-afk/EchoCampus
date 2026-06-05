from pydantic_settings import BaseSettings, SettingsConfigDict
from pathlib import Path



class Settings(BaseSettings):
    MILVUS_URI: str = "http://localhost:19530"

    model_config = SettingsConfigDict(
        env_file=Path(__file__).parent.parent.parent /".env",          
        env_file_encoding="utf-8" 
    )


settings = Settings()
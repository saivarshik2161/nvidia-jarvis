from pydantic_settings import BaseSettings
from typing import Optional
from functools import lru_cache


class Settings(BaseSettings):
    nvidia_api_base_url: str = "https://integrate.api.nvidia.com/v1"
    nvidia_api_key: str = ""
    nvidia_model: str = "nvidia/nemotron-3.5-lightning-30b-a3b"
    jarvis_client_token: str = ""
    database_url: str = "sqlite:///./jarvis.db"
    request_timeout: float = 10.0
    max_retries: int = 2

    class Config:
        env_file = ".env"
        case_sensitive = False


@lru_cache()
def get_settings() -> Settings:
    return Settings()
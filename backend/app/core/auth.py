from fastapi import HTTPException, Depends, Header
from typing import Optional
from app.core.config import get_settings


async def verify_token(authorization: Optional[str] = Header(None)) -> str:
    settings = get_settings()
    if not settings.jarvis_client_token:
        return "anonymous"

    if not authorization:
        raise HTTPException(status_code=401, detail="Missing authorization header")

    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization format")

    token = authorization[7:]
    if token != settings.jarvis_client_token:
        raise HTTPException(status_code=401, detail="Invalid token")

    return token
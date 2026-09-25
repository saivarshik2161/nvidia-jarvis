from fastapi import APIRouter, Depends, HTTPException, Request
from sqlalchemy.ext.asyncio import AsyncSession
from app.db.database import get_db
from app.schemas.interpret import (
    InterpretRequest,
    InterpretResponse,
    HealthResponse,
    MemoryCreate,
    MemoryResponse,
    MemoryListResponse,
    ErrorResponse,
)
from app.services.nvidia import nvidia_service
from app.services.memory import memory_service
from app.core.auth import verify_token


router = APIRouter()


@router.get("/health", response_model=HealthResponse)
async def health_check(request: Request):
    settings = request.app.state.settings if hasattr(request.app.state, "settings") else None
    nvidia_configured = bool(settings.nvidia_api_key) if settings else False
    return HealthResponse(
        status="healthy",
        version="1.0.0",
        nvidia_configured=nvidia_configured,
    )


@router.post("/api/v1/interpret", response_model=InterpretResponse)
async def interpret(
    request: InterpretRequest,
    db: AsyncSession = Depends(get_db),
    device_id: str = Depends(verify_token),
):
    if device_id != "anonymous":
        request.device_id = device_id

    response = await nvidia_service.interpret(request)

    if response.action == "remember":
        mem_service = memory_service(db)
        await mem_service.create_memory(request.device_id, MemoryCreate(content=response.params.memory_content or ""))

    elif response.action == "forget_memory":
        mem_service = memory_service(db)
        if response.params.clear_all:
            await mem_service.clear_memories(request.device_id)
        elif response.params.memory_id:
            await mem_service.delete_memory(request.device_id, response.params.memory_id)
        elif response.params.memory_content:
            mem = await mem_service.find_memory_by_content(request.device_id, response.params.memory_content)
            if mem:
                await mem_service.delete_memory(request.device_id, mem.id)

    return response


@router.post("/api/v1/memories", response_model=MemoryResponse, status_code=201)
async def create_memory(
    memory: MemoryCreate,
    db: AsyncSession = Depends(get_db),
    device_id: str = Depends(verify_token),
):
    mem_service = memory_service(db)
    return await mem_service.create_memory(device_id, memory)


@router.get("/api/v1/memories", response_model=MemoryListResponse)
async def list_memories(
    limit: int = 100,
    offset: int = 0,
    db: AsyncSession = Depends(get_db),
    device_id: str = Depends(verify_token),
):
    mem_service = memory_service(db)
    return await mem_service.get_memories(device_id, limit, offset)


@router.delete("/api/v1/memories/{memory_id}", status_code=204)
async def delete_memory(
    memory_id: str,
    db: AsyncSession = Depends(get_db),
    device_id: str = Depends(verify_token),
):
    mem_service = memory_service(db)
    deleted = await mem_service.delete_memory(device_id, memory_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Memory not found")


@router.delete("/api/v1/memories", status_code=204)
async def clear_memories(
    db: AsyncSession = Depends(get_db),
    device_id: str = Depends(verify_token),
):
    mem_service = memory_service(db)
    await mem_service.clear_memories(device_id)
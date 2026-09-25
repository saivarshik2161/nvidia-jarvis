from sqlalchemy import select, delete, func
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.memory import Memory
from app.schemas.interpret import MemoryCreate, MemoryResponse, MemoryListResponse
from typing import List, Optional
import uuid


class MemoryService:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def create_memory(self, device_id: str, memory: MemoryCreate) -> MemoryResponse:
        mem = Memory(
            device_id=device_id,
            content=memory.content,
        )
        self.db.add(mem)
        await self.db.commit()
        await self.db.refresh(mem)
        return MemoryResponse(**mem.to_dict())

    async def get_memories(self, device_id: str, limit: int = 100, offset: int = 0) -> MemoryListResponse:
        stmt = select(Memory).where(Memory.device_id == device_id).order_by(Memory.created_at.desc())
        count_stmt = select(func.count(Memory.id)).where(Memory.device_id == device_id)

        total_result = await self.db.execute(count_stmt)
        total = total_result.scalar() or 0

        stmt = stmt.limit(limit).offset(offset)
        result = await self.db.execute(stmt)
        memories = result.scalars().all()

        return MemoryListResponse(
            memories=[MemoryResponse(**m.to_dict()) for m in memories],
            total=total,
        )

    async def delete_memory(self, device_id: str, memory_id: str) -> bool:
        stmt = delete(Memory).where(Memory.id == memory_id, Memory.device_id == device_id)
        result = await self.db.execute(stmt)
        await self.db.commit()
        return result.rowcount > 0

    async def clear_memories(self, device_id: str) -> int:
        stmt = delete(Memory).where(Memory.device_id == device_id)
        result = await self.db.execute(stmt)
        await self.db.commit()
        return result.rowcount

    async def find_memory_by_content(self, device_id: str, content: str) -> Optional[Memory]:
        stmt = select(Memory).where(
            Memory.device_id == device_id,
            Memory.content.ilike(f"%{content}%")
        ).order_by(Memory.created_at.desc())
        result = await self.db.execute(stmt)
        return result.scalars().first()


memory_service = MemoryService
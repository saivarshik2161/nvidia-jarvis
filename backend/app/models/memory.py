from sqlalchemy import Column, String, DateTime, Text, Index
from sqlalchemy.sql import func
import uuid
from app.db.database import Base


class Memory(Base):
    __tablename__ = "memories"

    id = Column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    device_id = Column(String(255), nullable=False, index=True)
    content = Column(Text, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    __table_args__ = (
        Index("ix_memories_device_id_created_at", "device_id", "created_at"),
    )

    def to_dict(self):
        return {
            "id": self.id,
            "device_id": self.device_id,
            "content": self.content,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }
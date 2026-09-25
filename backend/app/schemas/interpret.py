from pydantic import BaseModel, Field, field_validator
from typing import Optional, Dict, Any, Literal
from datetime import datetime
import uuid


ActionType = Literal[
    "open_app",
    "call_contact",
    "send_message",
    "set_alarm",
    "remember",
    "forget_memory",
    "clarify",
    "unknown",
]


class InterpretRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=500)
    locale: str = Field(default="en-US", pattern=r"^[a-z]{2}-[A-Z]{2}$")
    device_id: str = Field(..., min_length=1, max_length=100)


class InterpretResponseParams(BaseModel):
    app_name: Optional[str] = None
    contact_name: Optional[str] = None
    phone_number: Optional[str] = None
    body: Optional[str] = None
    time: Optional[str] = None
    memory_content: Optional[str] = None
    memory_id: Optional[str] = None
    clear_all: Optional[bool] = None


class InterpretResponse(BaseModel):
    action: ActionType
    target: Optional[str] = None
    params: InterpretResponseParams = Field(default_factory=InterpretResponseParams)
    confidence: float = Field(..., ge=0.0, le=1.0)
    response_text: str = Field(..., min_length=1, max_length=500)
    requires_clarification: bool = False
    clarifying_question: Optional[str] = None

    @field_validator("confidence", mode="before")
    @classmethod
    def clamp_confidence(cls, v):
        if isinstance(v, (int, float)):
            return max(0.0, min(1.0, float(v)))
        return 0.0


class MemoryCreate(BaseModel):
    content: str = Field(..., min_length=1, max_length=2000)


class MemoryResponse(BaseModel):
    id: str
    device_id: str
    content: str
    created_at: str
    updated_at: str


class MemoryListResponse(BaseModel):
    memories: list[MemoryResponse]
    total: int


class HealthResponse(BaseModel):
    status: str = "healthy"
    version: str = "1.0.0"
    nvidia_configured: bool = False


class ErrorResponse(BaseModel):
    detail: str
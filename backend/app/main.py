from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import get_settings
from app.db.database import init_db, engine
from app.api.routes import router
from app.services.nvidia import nvidia_service


@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    app.state.settings = get_settings()
    yield
    await nvidia_service.close()
    await engine.dispose()


app = FastAPI(
    title="JARVIS Backend",
    description="Private Android Assistant Backend",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)


@app.get("/")
async def root():
    return {"name": "JARVIS", "version": "1.0.0", "status": "running"}
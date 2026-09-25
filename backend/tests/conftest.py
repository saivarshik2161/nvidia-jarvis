import pytest
import asyncio
from unittest.mock import AsyncMock, MagicMock
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.pool import StaticPool
from app.main import app
from app.db.database import Base, get_db
from app.core.config import Settings, get_settings
from app.services.nvidia import nvidia_service


@pytest.fixture(scope="session")
def event_loop():
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


@pytest.fixture(scope="function", autouse=True)
def clear_settings_cache():
    """Clear the settings cache before each test."""
    get_settings.cache_clear()
    yield
    get_settings.cache_clear()


@pytest.fixture(scope="function")
async def test_db():
    engine = create_async_engine(
        "sqlite+aiosqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    async_session = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    async with async_session() as session:
        yield session
    await engine.dispose()


@pytest.fixture(scope="function")
def mock_nvidia_service():
    """Mock the nvidia service to return controlled responses."""
    original_interpret = nvidia_service.interpret
    nvidia_service.interpret = AsyncMock()
    yield nvidia_service
    nvidia_service.interpret = original_interpret


@pytest.fixture(scope="function")
async def client(test_db, mock_nvidia_service, monkeypatch):
    monkeypatch.setenv("JARVIS_CLIENT_TOKEN", "test-token")
    monkeypatch.setenv("NVIDIA_API_KEY", "test-key")
    
    app.dependency_overrides[get_db] = lambda: test_db
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac
    app.dependency_overrides.clear()


@pytest.fixture(scope="function")
async def client_no_mock(test_db, monkeypatch):
    """Client without nvidia service mock - for auth tests."""
    monkeypatch.setenv("JARVIS_CLIENT_TOKEN", "test-token")
    monkeypatch.setenv("NVIDIA_API_KEY", "test-key")
    
    app.dependency_overrides[get_db] = lambda: test_db
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac
    app.dependency_overrides.clear()


@pytest.fixture
def auth_headers():
    return {"Authorization": "Bearer test-token"}


@pytest.fixture
def settings(monkeypatch):
    monkeypatch.setenv("JARVIS_CLIENT_TOKEN", "test-token")
    monkeypatch.setenv("NVIDIA_API_KEY", "test-key")
    monkeypatch.setenv("NVIDIA_API_BASE_URL", "https://integrate.api.nvidia.com/v1")
    monkeypatch.setenv("NVIDIA_MODEL", "nvidia/nemotron-3.5-lightning-30b-a3b")
    return Settings()
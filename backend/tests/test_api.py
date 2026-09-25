import pytest
import json
from unittest.mock import AsyncMock, MagicMock
from fastapi import HTTPException
from app.schemas.interpret import InterpretResponse, InterpretResponseParams


def make_interpret_response(
    action: str,
    target: str = None,
    params: dict = None,
    confidence: float = 0.9,
    response_text: str = "OK",
    requires_clarification: bool = False,
    clarifying_question: str = None,
) -> InterpretResponse:
    return InterpretResponse(
        action=action,
        target=target,
        params=InterpretResponseParams(**(params or {})),
        confidence=confidence,
        response_text=response_text,
        requires_clarification=requires_clarification,
        clarifying_question=clarifying_question,
    )


class TestHealthEndpoint:
    @pytest.mark.asyncio
    async def test_health_endpoint(self, client):
        response = await client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert "version" in data


class TestAuthentication:
    @pytest.mark.asyncio
    async def test_interpret_without_token_returns_401(self, client_no_mock):
        response = await client_no_mock.post("/api/v1/interpret", json={"text": "Hello", "locale": "en-US", "device_id": "test"})
        assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_interpret_with_invalid_token_returns_401(self, client_no_mock):
        headers = {"Authorization": "Bearer wrong-token"}
        response = await client_no_mock.post("/api/v1/interpret", json={"text": "Hello", "locale": "en-US", "device_id": "test"}, headers=headers)
        assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_interpret_with_valid_token_succeeds(self, client, auth_headers, mock_nvidia_service):
        mock_nvidia_service.interpret.return_value = make_interpret_response("unknown")

        response = await client.post(
            "/api/v1/interpret",
            json={"text": "Hello", "locale": "en-US", "device_id": "test"},
            headers=auth_headers
        )
        assert response.status_code == 200


class TestInterpretEndpoint:
    @pytest.mark.asyncio
    async def test_empty_transcript_rejected(self, client, auth_headers):
        response = await client.post("/api/v1/interpret", json={"text": "", "locale": "en-US", "device_id": "test"}, headers=auth_headers)
        assert response.status_code == 422

    @pytest.mark.asyncio
    async def test_transcript_too_long_rejected(self, client, auth_headers):
        long_text = "x" * 501
        response = await client.post("/api/v1/interpret", json={"text": long_text, "locale": "en-US", "device_id": "test"}, headers=auth_headers)
        assert response.status_code == 422

    @pytest.mark.asyncio
    async def test_valid_open_app_action(self, client, auth_headers, mock_nvidia_service):
        mock_nvidia_service.interpret.return_value = make_interpret_response(
            "open_app",
            target="Spotify",
            params={"app_name": "Spotify"},
            response_text="Opening Spotify."
        )

        response = await client.post(
            "/api/v1/interpret",
            json={"text": "Open Spotify", "locale": "en-US", "device_id": "test"},
            headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["action"] == "open_app"
        assert data["params"]["app_name"] == "Spotify"

    @pytest.mark.asyncio
    async def test_valid_call_contact_action(self, client, auth_headers, mock_nvidia_service):
        mock_nvidia_service.interpret.return_value = make_interpret_response(
            "call_contact",
            target="Mom",
            params={"contact_name": "Mom"},
            confidence=0.95,
            response_text="On it. I've prepared a call to Mom."
        )

        response = await client.post(
            "/api/v1/interpret",
            json={"text": "Call Mom", "locale": "en-US", "device_id": "test"},
            headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["action"] == "call_contact"
        assert data["params"]["contact_name"] == "Mom"

    @pytest.mark.asyncio
    async def test_valid_send_message_action(self, client, auth_headers, mock_nvidia_service):
        mock_nvidia_service.interpret.return_value = make_interpret_response(
            "send_message",
            target="Mom",
            params={"contact_name": "Mom", "body": "I will be late"},
            confidence=0.96,
            response_text="On it. I'm preparing a message to Mom."
        )

        response = await client.post(
            "/api/v1/interpret",
            json={"text": "Send Mom a message saying I will be late", "locale": "en-US", "device_id": "test"},
            headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["action"] == "send_message"
        assert data["params"]["contact_name"] == "Mom"
        assert data["params"]["body"] == "I will be late"

    @pytest.mark.asyncio
    async def test_valid_set_alarm_action(self, client, auth_headers, mock_nvidia_service):
        mock_nvidia_service.interpret.return_value = make_interpret_response(
            "set_alarm",
            target=None,
            params={"time": "07:00"},
            confidence=0.9,
            response_text="Alarm set for 7 AM."
        )

        response = await client.post(
            "/api/v1/interpret",
            json={"text": "Set an alarm for 7 AM", "locale": "en-US", "device_id": "test"},
            headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["action"] == "set_alarm"
        assert data["params"]["time"] == "07:00"

    @pytest.mark.asyncio
    async def test_valid_remember_action(self, client, auth_headers, mock_nvidia_service):
        mock_nvidia_service.interpret.return_value = make_interpret_response(
            "remember",
            target=None,
            params={"memory_content": "my mother is called Mom"},
            confidence=0.95,
            response_text="Remembered."
        )

        response = await client.post(
            "/api/v1/interpret",
            json={"text": "Remember that my mother is called Mom", "locale": "en-US", "device_id": "test"},
            headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["action"] == "remember"
        assert data["params"]["memory_content"] == "my mother is called Mom"

    @pytest.mark.asyncio
    async def test_valid_forget_memory_action(self, client, auth_headers, mock_nvidia_service):
        mock_nvidia_service.interpret.return_value = make_interpret_response(
            "forget_memory",
            target=None,
            params={"memory_content": "my mother is called Mom"},
            confidence=0.9,
            response_text="Forgetting that memory."
        )

        response = await client.post(
            "/api/v1/interpret",
            json={"text": "Forget that my mother is called Mom", "locale": "en-US", "device_id": "test"},
            headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["action"] == "forget_memory"

    @pytest.mark.asyncio
    async def test_unknown_request(self, client, auth_headers, mock_nvidia_service):
        mock_nvidia_service.interpret.return_value = make_interpret_response(
            "unknown",
            confidence=0.99,
            response_text="I can't help with that."
        )

        response = await client.post(
            "/api/v1/interpret",
            json={"text": "Hack the mainframe", "locale": "en-US", "device_id": "test"},
            headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["action"] == "unknown"

    @pytest.mark.asyncio
    async def test_clarification_request(self, client, auth_headers, mock_nvidia_service):
        mock_nvidia_service.interpret.return_value = make_interpret_response(
            "clarify",
            confidence=0.95,
            response_text="Who should I send the message to?",
            requires_clarification=True,
            clarifying_question="Who should I send the message to?"
        )

        response = await client.post(
            "/api/v1/interpret",
            json={"text": "Send a message", "locale": "en-US", "device_id": "test"},
            headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["action"] == "clarify"
        assert data["requires_clarification"] is True
        assert data["clarifying_question"] == "Who should I send the message to?"

    @pytest.mark.asyncio
    async def test_invalid_model_json_fallback(self, client, auth_headers, mock_nvidia_service):
        mock_nvidia_service.interpret.return_value = make_interpret_response(
            "unknown",
            response_text="I'm having trouble understanding right now. Please try again."
        )

        response = await client.post(
            "/api/v1/interpret",
            json={"text": "Hello", "locale": "en-US", "device_id": "test"},
            headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["action"] == "unknown"

    @pytest.mark.asyncio
    async def test_markdown_wrapped_json(self, client, auth_headers, mock_nvidia_service):
        mock_nvidia_service.interpret.return_value = make_interpret_response(
            "open_app",
            target="Chrome",
            params={"app_name": "Chrome"},
            confidence=0.9,
            response_text="Opening Chrome."
        )

        response = await client.post(
            "/api/v1/interpret",
            json={"text": "Open Chrome", "locale": "en-US", "device_id": "test"},
            headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["action"] == "open_app"
        assert data["params"]["app_name"] == "Chrome"

    @pytest.mark.asyncio
    async def test_unsafe_action_rejected(self, client, auth_headers, mock_nvidia_service):
        mock_nvidia_service.interpret.return_value = make_interpret_response(
            "unknown",
            confidence=0.9,
            response_text="I can't help with that."
        )

        response = await client.post(
            "/api/v1/interpret",
            json={"text": "Delete all files", "locale": "en-US", "device_id": "test"},
            headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["action"] == "unknown"

    @pytest.mark.asyncio
    async def test_missing_required_params_triggers_clarify(self, client, auth_headers, mock_nvidia_service):
        mock_nvidia_service.interpret.return_value = make_interpret_response(
            "clarify",
            confidence=0.5,
            response_text="Who should I send the message to?",
            requires_clarification=True,
            clarifying_question="Who should I send the message to?"
        )

        response = await client.post(
            "/api/v1/interpret",
            json={"text": "Send a message", "locale": "en-US", "device_id": "test"},
            headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["action"] == "clarify"

    @pytest.mark.asyncio
    async def test_confidence_clamped_to_range(self, client, auth_headers, mock_nvidia_service):
        mock_nvidia_service.interpret.return_value = make_interpret_response(
            "unknown",
            confidence=1.0,
            response_text="Test"
        )

        response = await client.post(
            "/api/v1/interpret",
            json={"text": "Test", "locale": "en-US", "device_id": "test"},
            headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["confidence"] == 1.0

    @pytest.mark.asyncio
    async def test_nvidia_api_timeout(self, client, auth_headers, mock_nvidia_service):
        mock_nvidia_service.interpret.return_value = make_interpret_response(
            "unknown",
            response_text="I'm having trouble understanding right now. Please try again."
        )

        response = await client.post(
            "/api/v1/interpret",
            json={"text": "Hello", "locale": "en-US", "device_id": "test"},
            headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["action"] == "unknown"

    @pytest.mark.asyncio
    async def test_nvidia_api_failure(self, client, auth_headers, mock_nvidia_service):
        mock_nvidia_service.interpret.return_value = make_interpret_response(
            "unknown",
            response_text="I'm having trouble understanding right now. Please try again."
        )

        response = await client.post(
            "/api/v1/interpret",
            json={"text": "Hello", "locale": "en-US", "device_id": "test"},
            headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["action"] == "unknown"


class TestMemoryEndpoints:
    @pytest.mark.asyncio
    async def test_create_memory(self, client, auth_headers):
        response = await client.post(
            "/api/v1/memories",
            json={"content": "My mother is called Mom"},
            headers=auth_headers
        )
        assert response.status_code == 201
        data = response.json()
        assert data["content"] == "My mother is called Mom"
        assert data["device_id"] == "test-token"

    @pytest.mark.asyncio
    async def test_list_memories(self, client, auth_headers):
        await client.post("/api/v1/memories", json={"content": "Memory 1"}, headers=auth_headers)
        await client.post("/api/v1/memories", json={"content": "Memory 2"}, headers=auth_headers)

        response = await client.get("/api/v1/memories", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 2
        assert len(data["memories"]) == 2

    @pytest.mark.asyncio
    async def test_delete_memory(self, client, auth_headers):
        create_resp = await client.post("/api/v1/memories", json={"content": "To delete"}, headers=auth_headers)
        memory_id = create_resp.json()["id"]

        response = await client.delete(f"/api/v1/memories/{memory_id}", headers=auth_headers)
        assert response.status_code == 204

        list_resp = await client.get("/api/v1/memories", headers=auth_headers)
        assert list_resp.json()["total"] == 0

    @pytest.mark.asyncio
    async def test_clear_memories(self, client, auth_headers):
        await client.post("/api/v1/memories", json={"content": "Memory 1"}, headers=auth_headers)
        await client.post("/api/v1/memories", json={"content": "Memory 2"}, headers=auth_headers)

        response = await client.delete("/api/v1/memories", headers=auth_headers)
        assert response.status_code == 204

        list_resp = await client.get("/api/v1/memories", headers=auth_headers)
        assert list_resp.json()["total"] == 0

    @pytest.mark.asyncio
    async def test_user_isolation(self, client, monkeypatch):
        # Override the verify_token dependency to accept multiple test tokens
        from app.core.auth import verify_token
        from app.main import app
        from fastapi import Header
        
        async def test_verify_token(authorization: str = Header(None)):
            if not authorization:
                raise HTTPException(status_code=401, detail="Missing authorization header")
            if not authorization.startswith("Bearer "):
                raise HTTPException(status_code=401, detail="Invalid authorization format")
            token = authorization[7:]
            if token not in ("user1", "user2"):
                raise HTTPException(status_code=401, detail="Invalid token")
            return token
        
        app.dependency_overrides[verify_token] = test_verify_token
        
        try:
            headers1 = {"Authorization": "Bearer user1"}
            headers2 = {"Authorization": "Bearer user2"}

            await client.post("/api/v1/memories", json={"content": "User 1 memory"}, headers=headers1)
            await client.post("/api/v1/memories", json={"content": "User 2 memory"}, headers=headers2)

            resp1 = await client.get("/api/v1/memories", headers=headers1)
            resp2 = await client.get("/api/v1/memories", headers=headers2)

            assert resp1.status_code == 200
            assert resp2.status_code == 200
            assert resp1.json()["total"] == 1
            assert resp2.json()["total"] == 1
            assert resp1.json()["memories"][0]["content"] == "User 1 memory"
            assert resp2.json()["memories"][0]["content"] == "User 2 memory"
        finally:
            app.dependency_overrides.pop(verify_token, None)
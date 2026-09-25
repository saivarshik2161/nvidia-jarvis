import json
import httpx
from typing import Optional
from app.core.config import get_settings
from app.schemas.interpret import InterpretRequest, InterpretResponse, InterpretResponseParams


SYSTEM_PROMPT = """You are JARVIS, a private Android assistant intent parser. Understand natural language, but return exactly one valid JSON object. Never return Markdown. Never return code. Never execute actions. Use only these actions: open_app, call_contact, send_message, set_alarm, remember, forget_memory, clarify, unknown. Ask for clarification when essential information is missing. Return unknown for unsupported requests. Never invent contact names or message bodies. Keep response_text concise, natural, and restricted to approved capabilities.

JSON schema:
{
  "action": "open_app|call_contact|send_message|set_alarm|remember|forget_memory|clarify|unknown",
  "target": "string|null",
  "params": {
    "app_name": "string|null",
    "contact_name": "string|null",
    "phone_number": "string|null",
    "body": "string|null",
    "time": "string|null",
    "memory_content": "string|null",
    "memory_id": "string|null",
    "clear_all": "boolean|null"
  },
  "confidence": 0.0-1.0,
  "response_text": "string",
  "requires_clarification": boolean,
  "clarifying_question": "string|null"
}

Examples:
User: "Call Mom"
{"action":"call_contact","target":"Mom","params":{"contact_name":"Mom"},"confidence":0.95,"response_text":"On it. I've prepared a call to Mom.","requires_clarification":false,"clarifying_question":null}

User: "Send Mom a message saying I'll be late"
{"action":"send_message","target":"Mom","params":{"contact_name":"Mom","body":"I'll be late"},"confidence":0.96,"response_text":"On it. I'm preparing a message to Mom.","requires_clarification":false,"clarifying_question":null}

User: "Set an alarm for 7 AM"
{"action":"set_alarm","target":null,"params":{"time":"07:00"},"confidence":0.9,"response_text":"Alarm set for 7 AM.","requires_clarification":false,"clarifying_question":null}

User: "Remember that my mother is called Mom"
{"action":"remember","target":null,"params":{"memory_content":"my mother is called Mom"},"confidence":0.95,"response_text":"Remembered.","requires_clarification":false,"clarifying_question":null}

User: "Forget that my mother is called Mom"
{"action":"forget_memory","target":null,"params":{"memory_content":"my mother is called Mom"},"confidence":0.9,"response_text":"Forgetting that memory.","requires_clarification":false,"clarifying_question":null}

User: "Open Spotify"
{"action":"open_app","target":"Spotify","params":{"app_name":"Spotify"},"confidence":0.9,"response_text":"Opening Spotify.","requires_clarification":false,"clarifying_question":null}

User: "Send a message"
{"action":"clarify","target":null,"params":{},"confidence":0.95,"response_text":"Who should I send the message to?","requires_clarification":true,"clarifying_question":"Who should I send the message to?"}

User: "Hack the mainframe"
{"action":"unknown","target":null,"params":{},"confidence":0.99,"response_text":"I can't help with that.","requires_clarification":false,"clarifying_question":null}"""


class NVIDIAService:
    def __init__(self):
        self.settings = get_settings()
        self.client: Optional[httpx.AsyncClient] = None

    async def _get_client(self) -> httpx.AsyncClient:
        if self.client is None or self.client.is_closed:
            self.client = httpx.AsyncClient(
                timeout=httpx.Timeout(self.settings.request_timeout),
                limits=httpx.Limits(max_connections=10),
            )
        return self.client

    async def close(self):
        if self.client and not self.client.is_closed:
            await self.client.aclose()

    def _extract_json(self, text: str) -> Optional[dict]:
        text = text.strip()
        if text.startswith("```"):
            lines = text.split("\n")
            if len(lines) >= 3:
                text = "\n".join(lines[1:-1])
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            return None

    def _validate_response(self, data: dict, request: InterpretRequest) -> InterpretResponse:
        action = data.get("action", "unknown")
        valid_actions = [
            "open_app", "call_contact", "send_message", "set_alarm",
            "remember", "forget_memory", "clarify", "unknown"
        ]
        if action not in valid_actions:
            action = "unknown"

        params_data = data.get("params", {})
        if not isinstance(params_data, dict):
            params_data = {}

        params = InterpretResponseParams(
            app_name=params_data.get("app_name"),
            contact_name=params_data.get("contact_name"),
            phone_number=params_data.get("phone_number"),
            body=params_data.get("body"),
            time=params_data.get("time"),
            memory_content=params_data.get("memory_content"),
            memory_id=params_data.get("memory_id"),
            clear_all=params_data.get("clear_all"),
        )

        confidence = data.get("confidence", 0.0)
        if not isinstance(confidence, (int, float)):
            confidence = 0.0
        confidence = max(0.0, min(1.0, float(confidence)))

        response_text = data.get("response_text", "I'm not sure how to help with that.")
        requires_clarification = data.get("requires_clarification", False)
        clarifying_question = data.get("clarifying_question")

        if action == "open_app" and not params.app_name:
            action = "clarify"
            response_text = "Which app would you like me to open?"
            requires_clarification = True
            clarifying_question = "Which app would you like me to open?"

        if action == "call_contact" and not params.contact_name:
            action = "clarify"
            response_text = "Who would you like me to call?"
            requires_clarification = True
            clarifying_question = "Who would you like me to call?"

        if action == "send_message":
            if not params.contact_name:
                action = "clarify"
                response_text = "Who should I send the message to?"
                requires_clarification = True
                clarifying_question = "Who should I send the message to?"
            elif not params.body:
                action = "clarify"
                response_text = "What should the message say?"
                requires_clarification = True
                clarifying_question = "What should the message say?"

        if action == "set_alarm" and not params.time:
            action = "clarify"
            response_text = "What time should I set the alarm for?"
            requires_clarification = True
            clarifying_question = "What time should I set the alarm for?"

        if action == "remember" and not params.memory_content:
            action = "clarify"
            response_text = "What would you like me to remember?"
            requires_clarification = True
            clarifying_question = "What would you like me to remember?"

        if action == "forget_memory" and not params.memory_content and not params.memory_id and not params.clear_all:
            action = "clarify"
            response_text = "What would you like me to forget?"
            requires_clarification = True
            clarifying_question = "What would you like me to forget?"

        target = data.get("target")
        if target == "":
            target = None

        return InterpretResponse(
            action=action,
            target=target,
            params=params,
            confidence=confidence,
            response_text=response_text,
            requires_clarification=requires_clarification,
            clarifying_question=clarifying_question,
        )

    async def interpret(self, request: InterpretRequest) -> InterpretResponse:
        if not self.settings.nvidia_api_key:
            return InterpretResponse(
                action="unknown",
                target=None,
                params=InterpretResponseParams(),
                confidence=0.0,
                response_text="Backend not configured. Please set NVIDIA_API_KEY.",
                requires_clarification=False,
                clarifying_question=None,
            )

        client = await self._get_client()
        headers = {
            "Authorization": f"Bearer {self.settings.nvidia_api_key}",
            "Content-Type": "application/json",
        }

        payload = {
            "model": self.settings.nvidia_model,
            "messages": [
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": request.text},
            ],
            "temperature": 0.1,
            "max_tokens": 500,
        }

        last_error = None
        for attempt in range(self.settings.max_retries + 1):
            try:
                response = await client.post(
                    f"{self.settings.nvidia_api_base_url}/chat/completions",
                    headers=headers,
                    json=payload,
                )
                response.raise_for_status()
                data = response.json()
                content = data["choices"][0]["message"]["content"]
                parsed = self._extract_json(content)
                if parsed is None:
                    raise ValueError("Failed to parse JSON from model response")
                return self._validate_response(parsed, request)
            except httpx.TimeoutException as e:
                last_error = e
                if attempt < self.settings.max_retries:
                    continue
            except httpx.HTTPStatusError as e:
                last_error = e
                if e.response.status_code >= 500 and attempt < self.settings.max_retries:
                    continue
                break
            except (KeyError, ValueError, json.JSONDecodeError) as e:
                last_error = e
                break
            except Exception as e:
                last_error = e
                break

        return InterpretResponse(
            action="unknown",
            target=None,
            params=InterpretResponseParams(),
            confidence=0.0,
            response_text="I'm having trouble understanding right now. Please try again.",
            requires_clarification=False,
            clarifying_question=None,
        )


nvidia_service = NVIDIAService()
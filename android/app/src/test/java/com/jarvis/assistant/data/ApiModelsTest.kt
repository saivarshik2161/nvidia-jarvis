package com.jarvis.assistant.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ApiModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testInterpretResponseParsing() {
        val jsonString = """
            {
                "action": "send_message",
                "target": "Mom",
                "params": {
                    "contact_name": "Mom",
                    "body": "I will be late"
                },
                "confidence": 0.96,
                "response_text": "On it. I'm preparing a message to Mom.",
                "requires_clarification": false,
                "clarifying_question": null
            }
        """

        val response = json.decodeFromString<InterpretResponse>(jsonString)

        assertEquals("send_message", response.action)
        assertEquals("Mom", response.target)
        assertEquals("Mom", response.params.contact_name)
        assertEquals("I will be late", response.params.body)
        assertEquals(0.96, response.confidence, 0.01)
        assertEquals("On it. I'm preparing a message to Mom.", response.response_text)
        assertFalse(response.requires_clarification)
        assertNull(response.clarifying_question)
    }

    @Test
    fun testInterpretResponseClarification() {
        val jsonString = """
            {
                "action": "clarify",
                "target": null,
                "params": {},
                "confidence": 0.95,
                "response_text": "Who should I send the message to?",
                "requires_clarification": true,
                "clarifying_question": "Who should I send the message to?"
            }
        """

        val response = json.decodeFromString<InterpretResponse>(jsonString)

        assertEquals("clarify", response.action)
        assertNull(response.target)
        assertTrue(response.requires_clarification)
        assertEquals("Who should I send the message to?", response.clarifying_question)
    }

    @Test
    fun testInterpretResponseUnknownAction() {
        val jsonString = """
            {
                "action": "unknown",
                "target": null,
                "params": {},
                "confidence": 0.99,
                "response_text": "I can't help with that.",
                "requires_clarification": false,
                "clarifying_question": null
            }
        """

        val response = json.decodeFromString<InterpretResponse>(jsonString)

        assertEquals("unknown", response.action)
        assertFalse(response.requires_clarification)
    }

    @Test
    fun testConfidenceClamping() {
        // Test that confidence values outside 0-1 are handled
        val jsonStringHigh = """
            {
                "action": "unknown",
                "target": null,
                "params": {},
                "confidence": 1.5,
                "response_text": "Test",
                "requires_clarification": false,
                "clarifying_question": null
            }
        """

        val jsonStringLow = """
            {
                "action": "unknown",
                "target": null,
                "params": {},
                "confidence": -0.5,
                "response_text": "Test",
                "requires_clarification": false,
                "clarifying_question": null
            }
        """

        val responseHigh = json.decodeFromString<InterpretResponse>(jsonStringHigh)
        val responseLow = json.decodeFromString<InterpretResponse>(jsonStringLow)

        // The parsing will accept the values; validation happens server-side
        assertEquals(1.5, responseHigh.confidence, 0.01)
        assertEquals(-0.5, responseLow.confidence, 0.01)
    }

    @Test
    fun testMemoryModels() {
        val memoryJson = """
            {
                "id": "test-id",
                "device_id": "device-1",
                "content": "My mother is called Mom",
                "created_at": "2026-01-01T00:00:00Z",
                "updated_at": "2026-01-01T00:00:00Z"
            }
        """

        val memory = json.decodeFromString<MemoryResponse>(memoryJson)

        assertEquals("test-id", memory.id)
        assertEquals("device-1", memory.device_id)
        assertEquals("My mother is called Mom", memory.content)
    }

    @Test
    fun testHealthResponse() {
        val healthJson = """
            {
                "status": "healthy",
                "version": "1.0.0",
                "nvidia_configured": true
            }
        """

        val health = json.decodeFromString<HealthResponse>(healthJson)

        assertEquals("healthy", health.status)
        assertEquals("1.0.0", health.version)
        assertTrue(health.nvidia_configured)
    }
}
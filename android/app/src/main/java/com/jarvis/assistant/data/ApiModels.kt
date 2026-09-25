package com.jarvis.assistant.data

import kotlinx.serialization.Serializable

@Serializable
data class InterpretRequest(
    val text: String,
    val locale: String = "en-US",
    val device_id: String
)

@Serializable
data class InterpretResponseParams(
    val app_name: String? = null,
    val contact_name: String? = null,
    val phone_number: String? = null,
    val body: String? = null,
    val time: String? = null,
    val memory_content: String? = null,
    val memory_id: String? = null,
    val clear_all: Boolean? = null
)

@Serializable
data class InterpretResponse(
    val action: String,
    val target: String?,
    val params: InterpretResponseParams,
    val confidence: Double,
    val response_text: String,
    val requires_clarification: Boolean,
    val clarifying_question: String?
)

@Serializable
data class MemoryCreate(
    val content: String
)

@Serializable
data class MemoryResponse(
    val id: String,
    val device_id: String,
    val content: String,
    val created_at: String,
    val updated_at: String
)

@Serializable
data class MemoryListResponse(
    val memories: List<MemoryResponse>,
    val total: Int
)

@Serializable
data class HealthResponse(
    val status: String,
    val version: String,
    val nvidia_configured: Boolean
)

@Serializable
data class ErrorResponse(
    val detail: String
)
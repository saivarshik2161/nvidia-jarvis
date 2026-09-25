package com.jarvis.assistant.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface JarvisApi {
    @GET("/health")
    suspend fun health(): HealthResponse

    @POST("/api/v1/interpret")
    suspend fun interpret(
        @Header("Authorization") auth: String,
        @Body request: InterpretRequest
    ): InterpretResponse

    @POST("/api/v1/memories")
    suspend fun createMemory(
        @Header("Authorization") auth: String,
        @Body memory: MemoryCreate
    ): MemoryResponse

    @GET("/api/v1/memories")
    suspend fun listMemories(
        @Header("Authorization") auth: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): MemoryListResponse

    @DELETE("/api/v1/memories/{memory_id}")
    suspend fun deleteMemory(
        @Header("Authorization") auth: String,
        @Path("memory_id") memoryId: String
    ): retrofit2.Response<Unit>

    @DELETE("/api/v1/memories")
    suspend fun clearMemories(
        @Header("Authorization") auth: String
    ): retrofit2.Response<Unit>
}
package com.jarvis.assistant.data

import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JarvisRepository @Inject constructor(
    private val api: JarvisApi
) {
    private val authToken: String = "Bearer ${BuildConfig.JARVIS_CLIENT_TOKEN}"

    suspend fun interpret(text: String, locale: String, deviceId: String): Result<InterpretResponse> {
        return try {
            val request = InterpretRequest(text = text, locale = locale, device_id = deviceId)
            val response = api.interpret(authToken, request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkHealth(): Result<HealthResponse> {
        return try {
            val response = api.health()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createMemory(content: String): Result<MemoryResponse> {
        return try {
            val response = api.createMemory(authToken, MemoryCreate(content))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listMemories(limit: Int = 100, offset: Int = 0): Result<MemoryListResponse> {
        return try {
            val response = api.listMemories(authToken, limit, offset)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMemory(memoryId: String): Result<Boolean> {
        return try {
            val response = api.deleteMemory(authToken, memoryId)
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearMemories(): Result<Boolean> {
        return try {
            val response = api.clearMemories(authToken)
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

object JarvisApiFactory {
    fun create(baseUrl: String): JarvisApi {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val json = Json { ignoreUnknownKeys = true }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(JarvisApi::class.java)
    }
}
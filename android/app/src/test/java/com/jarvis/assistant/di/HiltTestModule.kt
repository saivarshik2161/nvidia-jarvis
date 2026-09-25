package com.jarvis.assistant.di

import com.jarvis.assistant.data.JarvisApi
import com.jarvis.assistant.data.JarvisRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.mockk.mockk
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HiltTestModule {

    @Provides
    @Singleton
    fun provideJarvisApi(): JarvisApi {
        return mockk<JarvisApi>(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideJarvisRepository(api: JarvisApi): JarvisRepository {
        return JarvisRepository(api)
    }
}
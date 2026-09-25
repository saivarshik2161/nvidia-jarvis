package com.jarvis.assistant.di

import com.jarvis.assistant.BuildConfig
import com.jarvis.assistant.data.JarvisApi
import com.jarvis.assistant.data.JarvisApiFactory
import com.jarvis.assistant.data.JarvisRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJarvisApi(): JarvisApi {
        return JarvisApiFactory.create(BuildConfig.BACKEND_BASE_URL)
    }

    @Provides
    @Singleton
    fun provideJarvisRepository(api: JarvisApi): JarvisRepository {
        return JarvisRepository(api)
    }
}
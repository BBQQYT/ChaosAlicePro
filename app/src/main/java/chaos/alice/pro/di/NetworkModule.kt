package chaos.alice.pro.di

import chaos.alice.pro.data.network.PersonaApiService
import chaos.alice.pro.data.network.llm.GenericLlmApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton
import java.net.InetSocketAddress
import java.net.Proxy
import chaos.alice.pro.data.local.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import io.ktor.client.plugins.*

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    private fun getProxy(settingsRepository: SettingsRepository): Proxy? {
        val proxySettings = runBlocking { settingsRepository.proxySettings.first() }
        if (proxySettings.type != Proxy.Type.DIRECT && !proxySettings.host.isNullOrBlank() && proxySettings.port != null) {
            return Proxy(
                proxySettings.type,
                InetSocketAddress(proxySettings.host, proxySettings.port)
            )
        }
        return null
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(settingsRepository: SettingsRepository): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // Добавляем логгер

        getProxy(settingsRepository)?.let { clientBuilder.proxy(it) }

        return clientBuilder.build()
    }

    // --- НОВЫЙ МЕТОД ДЛЯ OKHTTPCLIENT ---
    @Provides
    @Singleton
    fun provideKtorClient(settingsRepository: SettingsRepository): HttpClient {
        return HttpClient(Android) {
            expectSuccess = false
            engine {
                proxy = getProxy(settingsRepository)
            }
        }
    }

    // --- ОБНОВЛЯЕМ МЕТОД ДЛЯ RETROFIT ---
    @Provides
    @Singleton
    fun provideRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit { // Добавляем okHttpClient
        return Retrofit.Builder()
            // Важно! Уберем базовый URL, чтобы он не мешал @Url в GenericLlmApiService
            .baseUrl("https://placeholder.com/") // Используем любую валидную заглушку
            .client(okHttpClient) // <-- ДОБАВЛЯЕМ НАШ HTTP КЛИЕНТ С ЛОГГЕРОМ
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun providePersonaApiService(retrofit: Retrofit): PersonaApiService {
        // Здесь базовый URL может быть важен. Давайте создадим отдельный Retrofit для него.
        // Это более правильный подход.
        val personaRetrofit = retrofit.newBuilder()
            .baseUrl("https://raw.githubusercontent.com/")
            .build()
        return personaRetrofit.create(PersonaApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGenericLlmApiService(retrofit: Retrofit): GenericLlmApiService {
        return retrofit.create(GenericLlmApiService::class.java)
    }
}
package chaos.alice.pro.di

import chaos.alice.pro.data.local.SettingsRepository
import chaos.alice.pro.data.network.PersonaApiService
import chaos.alice.pro.data.network.llm.GenericLlmApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.net.InetSocketAddress
import java.net.Proxy
import javax.inject.Singleton
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import io.ktor.client.engine.ProxyConfig
import io.ktor.client.engine.http
import io.ktor.http.URLProtocol

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    fun provideProxySettings(settingsRepository: SettingsRepository): SettingsRepository.ProxySettings {
        return runBlocking { settingsRepository.proxySettings.first() }
    }

    @Provides
    fun provideProxy(settingsRepository: SettingsRepository): Proxy? {
        return try {
            val proxySettings = runBlocking { settingsRepository.proxySettings.first() }
            if (proxySettings.type != Proxy.Type.DIRECT && !proxySettings.host.isNullOrBlank() && proxySettings.port != null) {
                Proxy(
                    proxySettings.type,
                    InetSocketAddress.createUnresolved(proxySettings.host, proxySettings.port)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    @Provides
    @Singleton
    @ProxyClient
    fun provideOkHttpClientWithProxy(settings: SettingsRepository.ProxySettings): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val clientBuilder = OkHttpClient.Builder().addInterceptor(loggingInterceptor)

        if (settings.type != Proxy.Type.DIRECT && !settings.host.isNullOrBlank() && settings.port != null) {
            val proxy = Proxy(
                settings.type,
                InetSocketAddress.createUnresolved(settings.host, settings.port)
            )
            clientBuilder.proxy(proxy)

            if (!settings.user.isNullOrBlank() && !settings.pass.isNullOrBlank()) {
                val authenticator = Authenticator { _, response ->
                    val credential = Credentials.basic(settings.user, settings.pass)
                    response.request.newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build()
                }
                clientBuilder.proxyAuthenticator(authenticator)
            }
        }
        return clientBuilder.build()
    }

    @Provides
    @Singleton
    @DirectClient
    fun provideOkHttpClientDirect(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        return OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
    }
    @Provides
    @Singleton
    fun provideKtorClient(settings: SettingsRepository.ProxySettings): HttpClient {
        return HttpClient(Android) {
            expectSuccess = false
            engine {
                if (settings.type != Proxy.Type.DIRECT && !settings.host.isNullOrBlank() && settings.port != null) {
                    proxy = ProxyBuilder.socks(settings.host, settings.port)
                }
            }
        }
    }

    @Provides
    @Singleton
    @ProxyClient
    fun provideRetrofitWithProxy(json: Json, @ProxyClient okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://placeholder.com/") // Заглушка
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    @DirectClient
    fun provideRetrofitDirect(json: Json, @DirectClient okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun providePersonaApiService(@DirectClient retrofit: Retrofit): PersonaApiService {
        return retrofit.create(PersonaApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGenericLlmApiService(@ProxyClient retrofit: Retrofit): GenericLlmApiService {
        return retrofit.create(GenericLlmApiService::class.java)
    }

    @Provides
    @Singleton
    @CheckerClient
    fun provideOkHttpClientForChecker(settings: SettingsRepository.ProxySettings): OkHttpClient {
        val clientBuilder = OkHttpClient.Builder()

        if (settings.type != Proxy.Type.DIRECT && !settings.host.isNullOrBlank() && settings.port != null) {
            val proxy = Proxy(settings.type, InetSocketAddress.createUnresolved(settings.host, settings.port))
            clientBuilder.proxy(proxy)
            if (!settings.user.isNullOrBlank() && !settings.pass.isNullOrBlank()) {
                clientBuilder.proxyAuthenticator { _, response ->
                    val credential = Credentials.basic(settings.user, settings.pass)
                    response.request.newBuilder().header("Proxy-Authorization", credential).build()
                }
            }
        }
        return clientBuilder.build()
    }
}
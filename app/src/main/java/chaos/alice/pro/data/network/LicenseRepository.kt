package chaos.alice.pro.data.network

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: HttpClient
) {
    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    // Единственный метод, который нам нужен
    suspend fun getLicenseResponse(deviceId: String): HttpResponse {
        val url = "https://raw.githubusercontent.com/BBQQYT/CA-promt/main/defender/$deviceId"
        return client.get(url)
    }
}
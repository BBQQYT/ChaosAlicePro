package chaos.alice.pro.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import chaos.alice.pro.data.settingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.net.Proxy
import chaos.alice.pro.data.models.ResponseLength
import chaos.alice.pro.data.models.AppTheme
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

@Singleton
class SettingsRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val disclaimersShownKey = booleanPreferencesKey("disclaimers_shown")
    private val proxyTypeKey = stringPreferencesKey("proxy_type")
    private val proxyHostKey = stringPreferencesKey("proxy_host")
    private val proxyPortKey = intPreferencesKey("proxy_port")
    
    // Эти ключи теперь только для названия в SecurePrefs
    private val proxyUserKey = "proxy_user"
    private val proxyPasswordKey = "proxy_password"

    private val modelNameKey = stringPreferencesKey("model_name")
    private val responseLengthKey = stringPreferencesKey("response_length")
    private val appThemeKey = stringPreferencesKey("app_theme")

    data class ProxySettings(
        val type: Proxy.Type = Proxy.Type.DIRECT,
        val host: String? = null,
        val port: Int? = null,
        val user: String? = null,
        val pass: String? = null
    )

    val proxySettings: Flow<ProxySettings> = context.settingsDataStore.data.map { prefs ->
        val type = try {
            Proxy.Type.valueOf(prefs[proxyTypeKey] ?: Proxy.Type.DIRECT.name)
        } catch (_: IllegalArgumentException) {
            Proxy.Type.DIRECT
        }
        ProxySettings(
            type = type,
            host = prefs[proxyHostKey],
            port = prefs[proxyPortKey],
            user = securePrefs.getString(proxyUserKey, null),
            pass = securePrefs.getString(proxyPasswordKey, null)
        )
    }
    
    // ... остальной код (appTheme, responseLength и т.д.)
    
    val appTheme: Flow<AppTheme> = context.settingsDataStore.data.map { preferences ->
        val themeName = preferences[appThemeKey] ?: AppTheme.DEFAULT.name
        try {
            AppTheme.valueOf(themeName)
        } catch (_: IllegalArgumentException) {
            AppTheme.DEFAULT
        }
    }

    val responseLength: Flow<ResponseLength> = context.settingsDataStore.data.map { preferences ->
        val lengthName = preferences[responseLengthKey] ?: ResponseLength.AUTO.name
        try {
            ResponseLength.valueOf(lengthName)
        } catch (_: IllegalArgumentException) {
            ResponseLength.AUTO
        }
    }

    val haveDisclaimersBeenShown: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[disclaimersShownKey] ?: false
    }

    fun getModelName(): Flow<String?> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[modelNameKey]
        }
    }

    suspend fun saveAppTheme(theme: AppTheme) {
        context.settingsDataStore.edit { preferences ->
            preferences[appThemeKey] = theme.name
        }
    }

    suspend fun saveResponseLength(length: ResponseLength) {
        context.settingsDataStore.edit { preferences ->
            preferences[responseLengthKey] = length.name
        }
    }

    suspend fun saveProxySettings(settings: ProxySettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[proxyTypeKey] = settings.type.name
            settings.host?.let { prefs[proxyHostKey] = it } ?: prefs.remove(proxyHostKey)
            settings.port?.let { prefs[proxyPortKey] = it } ?: prefs.remove(proxyPortKey)
        }
        securePrefs.edit().apply {
            putString(proxyUserKey, settings.user)
            putString(proxyPasswordKey, settings.pass)
            apply()
        }
    }

    suspend fun saveModelName(modelName: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[modelNameKey] = modelName
        }
    }

    suspend fun markDisclaimersAsShown() {
        context.settingsDataStore.edit { settings ->
            settings[disclaimersShownKey] = true
        }
    }
}

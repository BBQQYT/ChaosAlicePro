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

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val DISCLAIMERS_SHOWN_KEY = booleanPreferencesKey("disclaimers_shown")
    private val PROXY_TYPE_KEY = stringPreferencesKey("proxy_type")
    private val PROXY_HOST_KEY = stringPreferencesKey("proxy_host")
    private val PROXY_PORT_KEY = intPreferencesKey("proxy_port")
    private val PROXY_USER_KEY = stringPreferencesKey("proxy_user")
    private val PROXY_PASSWORD_KEY = stringPreferencesKey("proxy_password")

    private val MODEL_NAME_KEY = stringPreferencesKey("model_name")
    private val RESPONSE_LENGTH_KEY = stringPreferencesKey("response_length")
    private val APP_THEME_KEY = stringPreferencesKey("app_theme")

    data class ProxySettings(
        val type: Proxy.Type = Proxy.Type.DIRECT,
        val host: String? = null,
        val port: Int? = null,
        val user: String? = null,
        val pass: String? = null
    )

    val proxySettings: Flow<ProxySettings> = context.settingsDataStore.data.map { prefs ->
        val type = try {
            Proxy.Type.valueOf(prefs[PROXY_TYPE_KEY] ?: Proxy.Type.DIRECT.name)
        } catch (e: IllegalArgumentException) {
            Proxy.Type.DIRECT
        }
        ProxySettings(
            type = type,
            host = prefs[PROXY_HOST_KEY],
            port = prefs[PROXY_PORT_KEY],
            user = prefs[PROXY_USER_KEY],
            pass = prefs[PROXY_PASSWORD_KEY]
        )
    }

    val appTheme: Flow<AppTheme> = context.settingsDataStore.data.map { preferences ->
        val themeName = preferences[APP_THEME_KEY] ?: AppTheme.DEFAULT.name
        try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.DEFAULT
        }
    }

    val responseLength: Flow<ResponseLength> = context.settingsDataStore.data.map { preferences ->
        val lengthName = preferences[RESPONSE_LENGTH_KEY] ?: ResponseLength.AUTO.name
        try {
            ResponseLength.valueOf(lengthName)
        } catch (e: IllegalArgumentException) {
            ResponseLength.AUTO
        }
    }

    val haveDisclaimersBeenShown: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[DISCLAIMERS_SHOWN_KEY] ?: false
    }

    fun getModelName(): Flow<String?> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[MODEL_NAME_KEY]
        }
    }

    suspend fun saveAppTheme(theme: AppTheme) {
        context.settingsDataStore.edit { preferences ->
            preferences[APP_THEME_KEY] = theme.name
        }
    }

    suspend fun saveResponseLength(length: ResponseLength) {
        context.settingsDataStore.edit { preferences ->
            preferences[RESPONSE_LENGTH_KEY] = length.name
        }
    }

    suspend fun saveProxySettings(settings: ProxySettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[PROXY_TYPE_KEY] = settings.type.name
            settings.host?.let { prefs[PROXY_HOST_KEY] = it } ?: prefs.remove(PROXY_HOST_KEY)
            settings.port?.let { prefs[PROXY_PORT_KEY] = it } ?: prefs.remove(PROXY_PORT_KEY)
            settings.user?.let { prefs[PROXY_USER_KEY] = it } ?: prefs.remove(PROXY_USER_KEY)
            settings.pass?.let { prefs[PROXY_PASSWORD_KEY] = it } ?: prefs.remove(PROXY_PASSWORD_KEY)
        }
    }

    suspend fun saveModelName(modelName: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[MODEL_NAME_KEY] = modelName
        }
    }

    suspend fun markDisclaimersAsShown() {
        context.settingsDataStore.edit { settings ->
            settings[DISCLAIMERS_SHOWN_KEY] = true
        }
    }
}
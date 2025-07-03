package chaos.alice.pro.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import chaos.alice.pro.data.settingsDataStore // Убедитесь, что импорт правильный
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.net.Proxy

// ЭТОЙ СТРОКИ ЗДЕСЬ БЫТЬ НЕ ДОЛЖНО:
// private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val PIRACY_FLAG = intPreferencesKey("piracy_flag")
    private val LAUNCHES_SINCE_LAST_CHECK = intPreferencesKey("launches_since_last_check")
    private val DISCLAIMERS_SHOWN_KEY = booleanPreferencesKey("disclaimers_shown")

    private val PROXY_TYPE_KEY = stringPreferencesKey("proxy_type")
    private val PROXY_HOST_KEY = stringPreferencesKey("proxy_host")
    private val PROXY_PORT_KEY = intPreferencesKey("proxy_port")

    data class ProxySettings(
        val type: Proxy.Type = Proxy.Type.DIRECT,
        val host: String? = null,
        val port: Int? = null
    )

    val proxySettings: Flow<ProxySettings> = context.settingsDataStore.data.map { prefs ->
        val type = Proxy.Type.valueOf(prefs[PROXY_TYPE_KEY] ?: Proxy.Type.DIRECT.name)
        ProxySettings(
            type = type,
            host = prefs[PROXY_HOST_KEY],
            port = prefs[PROXY_PORT_KEY]
        )
    }


    val isMarkedAsPirate: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        (preferences[PIRACY_FLAG] ?: 0) > 0
    }

    val launchesSinceLastCheck: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[LAUNCHES_SINCE_LAST_CHECK] ?: 0
    }

    suspend fun markAsPirate() {
        context.settingsDataStore.edit { settings ->
            settings[PIRACY_FLAG] = 1
        }
    }

    val haveDisclaimersBeenShown: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[DISCLAIMERS_SHOWN_KEY] ?: false
    }

    suspend fun saveProxySettings(settings: ProxySettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[PROXY_TYPE_KEY] = settings.type.name
            settings.host?.let { prefs[PROXY_HOST_KEY] = it } ?: prefs.remove(PROXY_HOST_KEY)
            settings.port?.let { prefs[PROXY_PORT_KEY] = it } ?: prefs.remove(PROXY_PORT_KEY)
        }
    }

    suspend fun markDisclaimersAsShown() {
        context.settingsDataStore.edit { settings ->
            settings[DISCLAIMERS_SHOWN_KEY] = true
        }
    }

    suspend fun unmarkAsPirate() {
        context.settingsDataStore.edit { settings ->
            settings[PIRACY_FLAG] = 0
        }
    }

    suspend fun incrementLaunchesSinceLastCheck() {
        context.settingsDataStore.edit { settings ->
            val currentCount = settings[LAUNCHES_SINCE_LAST_CHECK] ?: 0
            settings[LAUNCHES_SINCE_LAST_CHECK] = currentCount + 1
        }
    }

    suspend fun resetLaunchesSinceLastCheck() {
        context.settingsDataStore.edit { settings ->
            settings[LAUNCHES_SINCE_LAST_CHECK] = 0
        }
    }
}
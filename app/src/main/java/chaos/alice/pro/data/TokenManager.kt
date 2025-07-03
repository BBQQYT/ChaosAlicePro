package chaos.alice.pro.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import chaos.alice.pro.data.models.ApiProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ЭТОЙ СТРОКИ ЗДЕСЬ БЫТЬ НЕ ДОЛЖНО:
// private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")


@Singleton
class TokenManager @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("api_token")
        private val PROVIDER_KEY = stringPreferencesKey("api_provider")
        private val MODEL_NAME_KEY = stringPreferencesKey("model_name") // <-- НОВЫЙ КЛЮЧ
    }

    fun getToken(): Flow<String?> {
        // Используем общий data store
        return context.settingsDataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }
    }

    fun getProvider(): Flow<ApiProvider> {
        // Используем общий data store
        return context.settingsDataStore.data.map { preferences ->
            val providerName = preferences[PROVIDER_KEY] ?: ApiProvider.OPEN_ROUTER.name
            ApiProvider.valueOf(providerName)
        }
    }

    fun getModelName(): Flow<String?> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[MODEL_NAME_KEY]
        }
    }

    // --- ОБНОВЛЕННАЯ ФУНКЦИЯ СОХРАНЕНИЯ ---
    suspend fun saveSettings(token: String, provider: ApiProvider, model: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[PROVIDER_KEY] = provider.name
            preferences[MODEL_NAME_KEY] = model
        }
    }

    // Старый метод можно либо удалить, либо оставить для обратной совместимости
    suspend fun saveTokenAndProvider(token: String, provider: ApiProvider) {
        saveSettings(token, provider, "") // Сохраняем с пустой моделью
    }
}

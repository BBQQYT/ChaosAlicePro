package chaos.alice.pro.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import chaos.alice.pro.data.models.ApiProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private val ACTIVE_PROVIDER_KEY = stringPreferencesKey("active_api_provider")
        private val GEMINI_TOKEN_KEY = stringPreferencesKey("gemini_api_token")
        private val OPENAI_TOKEN_KEY = stringPreferencesKey("openai_api_token")
        private val OPENROUTER_TOKEN_KEY = stringPreferencesKey("openrouter_api_token")
    }

    fun getActiveProvider(): Flow<ApiProvider> {
        return context.settingsDataStore.data.map { preferences ->
            val providerName = preferences[ACTIVE_PROVIDER_KEY] ?: ApiProvider.GEMINI.name
            try {
                ApiProvider.valueOf(providerName)
            } catch (e: IllegalArgumentException) {
                ApiProvider.GEMINI
            }
        }
    }

    fun getGeminiKey(): Flow<String?> = context.settingsDataStore.data.map { it[GEMINI_TOKEN_KEY] }
    fun getOpenAiKey(): Flow<String?> = context.settingsDataStore.data.map { it[OPENAI_TOKEN_KEY] }
    fun getOpenRouterKey(): Flow<String?> = context.settingsDataStore.data.map { it[OPENROUTER_TOKEN_KEY] }

    suspend fun saveSettings(
        activeProvider: ApiProvider,
        geminiKey: String,
        openAiKey: String,
        openRouterKey: String
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[ACTIVE_PROVIDER_KEY] = activeProvider.name
            preferences[GEMINI_TOKEN_KEY] = geminiKey
            preferences[OPENAI_TOKEN_KEY] = openAiKey
            preferences[OPENROUTER_TOKEN_KEY] = openRouterKey
        }
    }
}
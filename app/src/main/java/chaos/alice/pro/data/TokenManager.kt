package chaos.alice.pro.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import chaos.alice.pro.data.models.ApiProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(@param:ApplicationContext private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "secure_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _geminiKey = MutableStateFlow(securePrefs.getString(GEMINI_TOKEN_KEY, null))
    private val _openAiKey = MutableStateFlow(securePrefs.getString(OPENAI_TOKEN_KEY, null))
    private val _openRouterKey = MutableStateFlow(securePrefs.getString(OPENROUTER_TOKEN_KEY, null))

    companion object {
        private val ACTIVE_PROVIDER_KEY = stringPreferencesKey("active_api_provider")
        private const val GEMINI_TOKEN_KEY = "gemini_api_token"
        private const val OPENAI_TOKEN_KEY = "openai_api_token"
        private const val OPENROUTER_TOKEN_KEY = "openrouter_api_token"
    }

    fun getActiveProvider(): Flow<ApiProvider> {
        return context.settingsDataStore.data.map { preferences ->
            val providerName = preferences[ACTIVE_PROVIDER_KEY] ?: ApiProvider.GEMINI.name
            try {
                ApiProvider.valueOf(providerName)
            } catch (_: IllegalArgumentException) {
                ApiProvider.GEMINI
            }
        }
    }

    fun getGeminiKey(): Flow<String?> = _geminiKey
    fun getOpenAiKey(): Flow<String?> = _openAiKey
    fun getOpenRouterKey(): Flow<String?> = _openRouterKey

    suspend fun saveSettings(
        activeProvider: ApiProvider,
        geminiKey: String,
        openAiKey: String,
        openRouterKey: String
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[ACTIVE_PROVIDER_KEY] = activeProvider.name
        }
        
        securePrefs.edit().apply {
            putString(GEMINI_TOKEN_KEY, geminiKey)
            putString(OPENAI_TOKEN_KEY, openAiKey)
            putString(OPENROUTER_TOKEN_KEY, openRouterKey)
            apply()
        }
        _geminiKey.value = geminiKey
        _openAiKey.value = openAiKey
        _openRouterKey.value = openRouterKey
    }
    
    fun clearAll() {
        securePrefs.edit().clear().apply()
        _geminiKey.value = null
        _openAiKey.value = null
        _openRouterKey.value = null
    }
}

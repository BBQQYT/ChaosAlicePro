package chaos.alice.pro.data

import android.util.Log
import chaos.alice.pro.data.network.Persona
import chaos.alice.pro.data.network.PersonaApiService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonaRepository @Inject constructor(private val apiService: PersonaApiService) {

    private var cachedOfficialPersonas: List<Persona>? = null
    private var cachedCustomPersonas: List<Persona>? = null
    private val mutex = Mutex()

    suspend fun getAllPersonas(): Pair<List<Persona>, List<Persona>> {
        return mutex.withLock {
            if (cachedOfficialPersonas == null || cachedOfficialPersonas!!.isEmpty()) {
                try {
                    val official = apiService.getOfficialPersonas()
                    if (official.isNotEmpty()) {
                        cachedOfficialPersonas = official
                    }
                } catch (e: Exception) {
                    Log.e("PersonaRepository", "Failed to load official personas", e)
                }
            }
            if (cachedCustomPersonas == null || cachedCustomPersonas!!.isEmpty()) {
                try {
                    val custom = apiService.getCustomPersonas()
                    if (custom.isNotEmpty()) {
                        cachedCustomPersonas = custom
                    }
                } catch (e: Exception) {
                    Log.e("PersonaRepository", "Failed to load custom personas", e)
                }
            }
            Pair(cachedOfficialPersonas ?: emptyList(), cachedCustomPersonas ?: emptyList())
        }
    }

    suspend fun getPersonaById(id: String): Persona? {
        val (official, custom) = getAllPersonas()
        val allPersonas = official + custom
        val persona = allPersonas.find { it.id == id }
        persona?.let {
            if (it.prompt == null) {
                mutex.withLock {
                    if (it.prompt == null) {
                        try {
                            it.prompt = apiService.getPrompt(it.prompt_url).string()
                        } catch (e: Exception) {
                            // Сеть могла отвалиться (VPN/DNS). Не роняем приложение —
                            // prompt остаётся null и будет повторно загружен при следующем вызове.
                            Log.e("PersonaRepository", "Failed to load prompt for ${it.id}", e)
                        }
                    }
                }
            }
        }
        return persona
    }
}
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

    // Теперь кешируем два списка
    private var cachedOfficialPersonas: List<Persona>? = null
    private var cachedCustomPersonas: List<Persona>? = null
    private val mutex = Mutex()

    // Метод для получения ОБОИХ списков
    suspend fun getAllPersonas(): Pair<List<Persona>, List<Persona>> {
        return mutex.withLock {
            if (cachedOfficialPersonas == null) {
                cachedOfficialPersonas = try {
                    apiService.getOfficialPersonas()
                } catch (e: Exception) {
                    Log.e("PersonaRepository", "Failed to load official personas", e)
                    emptyList() // В случае ошибки возвращаем пустой список
                }
            }
            if (cachedCustomPersonas == null) {
                cachedCustomPersonas = try {
                    apiService.getCustomPersonas()
                } catch (e: Exception) {
                    Log.e("PersonaRepository", "Failed to load custom personas", e)
                    emptyList() // В случае ошибки возвращаем пустой список
                }
            }
            Pair(cachedOfficialPersonas!!, cachedCustomPersonas!!)
        }
    }

    suspend fun getPersonaById(id: String): Persona? {
        val (official, custom) = getAllPersonas()
        val allPersonas = official + custom
        val persona = allPersonas.find { it.id == id }
        persona?.let {
            if (it.prompt == null) {
                mutex.withLock {
                    // Повторная проверка, вдруг другой поток уже загрузил
                    if (it.prompt == null) {
                        it.prompt = apiService.getPrompt(it.prompt_url).string()
                    }
                }
            }
        }
        return persona
    }
}
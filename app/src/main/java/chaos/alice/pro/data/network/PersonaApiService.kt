package chaos.alice.pro.data.network
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

interface PersonaApiService {
    // Старый метод для официальных
    @GET("https://raw.githubusercontent.com/BBQQYT/CA-promt/main/pers.json")
    suspend fun getOfficialPersonas(): List<Persona>

    // Новый метод для кастомных
    @GET("https://raw.githubusercontent.com/BBQQYT/CA-promt/main/cus_pers.json")
    suspend fun getCustomPersonas(): List<Persona>

    @GET
    suspend fun getPrompt(@Url url: String): ResponseBody
}
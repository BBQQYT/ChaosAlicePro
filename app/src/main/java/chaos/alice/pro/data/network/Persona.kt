package chaos.alice.pro.data.network

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Persona(
    val id: String,
    val name: String,
    val icon_url: String,
    val description: String,
    val prompt_url: String,
    var prompt: String? = null
)
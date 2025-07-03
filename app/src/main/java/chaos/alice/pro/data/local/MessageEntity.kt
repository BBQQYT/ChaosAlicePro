package chaos.alice.pro.data.local

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import chaos.alice.pro.data.models.ApiProvider

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,
    val text: String,
    val sender: Sender,
    val timestamp: Long,
    val isError: Boolean = false
) {

    @Ignore
    var apiProvider: ApiProvider = ApiProvider.GEMINI

    @Ignore
    var modelName: String? = null
}

enum class Sender {
    USER, MODEL
}
package chaos.alice.pro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val personaId: String, // ID персонажа из JSON
    val createdAt: Long = System.currentTimeMillis()
)
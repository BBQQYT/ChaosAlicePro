package chaos.alice.pro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.room.Transaction

@Dao
interface ChatDao {
    // Методы для сообщений
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    // НОВЫЕ методы для чатов
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity): Long // Возвращает ID созданного чата

    @Query("SELECT * FROM chats ORDER BY createdAt DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: Long): ChatEntity? // Этот уже есть, он suspend

    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun getChatFlow(chatId: Long): Flow<ChatEntity?> // А этот возвращает Flow

    @Query("UPDATE chats SET title = :newTitle WHERE id = :chatId")
    suspend fun updateChatTitle(chatId: Long, newTitle: String)
    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: Long)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesByChatId(chatId: Long)

    // 👇 ДОБАВЬТЕ ЭТОТ НОВЫЙ МЕТОД
    @Transaction
    suspend fun deleteChatAndMessages(chatId: Long) {
        deleteChatById(chatId)
        deleteMessagesByChatId(chatId)
    }
}
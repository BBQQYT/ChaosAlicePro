package chaos.alice.pro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface ChatDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity): Long

    @Query("SELECT * FROM chats ORDER BY createdAt DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: Long): ChatEntity?

    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun getChatFlow(chatId: Long): Flow<ChatEntity?>

    @Query("UPDATE chats SET title = :newTitle WHERE id = :chatId")
    suspend fun updateChatTitle(chatId: Long, newTitle: String)
    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: Long)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesByChatId(chatId: Long)

    @Transaction
    suspend fun deleteChatAndMessages(chatId: Long) {
        deleteChatById(chatId)
        deleteMessagesByChatId(chatId)
    }

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET text = :newText WHERE id = :messageId")
    suspend fun updateMessageText(messageId: Long, newText: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: Long)

    @Query("DELETE FROM messages WHERE chatId = :chatId AND timestamp > :timestamp")
    suspend fun deleteMessagesAfter(chatId: Long, timestamp: Long)

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Long): MessageEntity?
}
package chaos.alice.pro.data.network.llm

import chaos.alice.pro.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow

interface LlmProvider {

    suspend fun generateResponseStream(
        apiKey: String,
        systemPrompt: String?,
        history: List<MessageEntity>,
        userMessage: MessageEntity
    ): Flow<String>
}
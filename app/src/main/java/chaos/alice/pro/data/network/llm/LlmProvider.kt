package chaos.alice.pro.data.network.llm

import chaos.alice.pro.data.local.MessageEntity

/**
 * Унифицированный интерфейс для взаимодействия с разными LLM.
 */
interface LlmProvider {
    /**
     * Отправляет сообщение и историю чата в LLM и возвращает ответ.
     * @param apiKey API ключ.
     * @param systemPrompt Системный промпт (персона).
     * @param history История сообщений.
     * @param userMessage Последнее сообщение от пользователя.
     * @return Текст ответа от модели.
     * @throws Exception если что-то пошло не так.
     */
    suspend fun generateResponse(
        apiKey: String,
        systemPrompt: String?,
        history: List<MessageEntity>,
        userMessage: MessageEntity
    ): String
}
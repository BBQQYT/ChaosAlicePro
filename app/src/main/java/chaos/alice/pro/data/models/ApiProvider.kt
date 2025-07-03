package chaos.alice.pro.data.models

// Enum для всех поддерживаемых провайдеров API
enum class ApiProvider(val displayName: String) {
    GEMINI("Google Gemini"),
    OPEN_ROUTER("OpenRouter"),
    OPEN_AI("OpenAI"),
    TOGETHER("Together.ai"),
    QWEN("Qwen (Alibaba)"),
    DEEPSEEK("Deepseek")
}
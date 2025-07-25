package chaos.alice.pro.data.network.llm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import chaos.alice.pro.data.local.MessageEntity
import chaos.alice.pro.data.local.Sender
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GeminiLlmProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : LlmProvider {

    override suspend fun generateResponseStream(
        apiKey: String,
        systemPrompt: String?,
        history: List<MessageEntity>,
        userMessage: MessageEntity
    ): Flow<String> {

        val modelName = userMessage.modelName ?: "gemini-1.5-flash"

        val model = GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            systemInstruction = content { text(systemPrompt ?: "") }
        )

        val fullHistory = buildList {
            history.forEach { message ->
                if (message.imageUri == null) {
                    add(content(if (message.sender == Sender.USER) "user" else "model") { text(message.text) })
                }
            }
            add(content("user") {
                userMessage.imageUri?.let {
                    try {
                        image(it.toUri().toBitmap(context))
                    } catch (e: Exception) {
                        Log.e("GeminiProvider", "Image Load Error", e)
                    }
                }
                text(userMessage.text)
            })
        }

        return model.generateContentStream(*fullHistory.toTypedArray())
            .map { response -> response.text ?: "" }
    }

    private fun Uri.toBitmap(context: Context): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, this))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, this)
        }
    }
}
package chaos.alice.pro.ui.chatlist

import chaos.alice.pro.data.local.ChatEntity
import chaos.alice.pro.data.network.Persona

data class ChatWithPersona(
    val chat: ChatEntity,
    val persona: Persona?
)
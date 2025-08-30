package com.aiyu

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable


@Serializable
data class Message(
    val message: String,
    val time: Long = System.currentTimeMillis()
)

class LocalDb private constructor() {
    private var _messages: MutableStateFlow<List<Message>> = MutableStateFlow<List<Message>>(emptyList())
    val message: Flow<List<Message>> = _messages

    fun addMessage(message: String) {
        _messages.value += Message(message)
    }

    fun updateMessage(oldMessage: String, newMessage: String) {
        _messages.value = _messages.value.map {
            if (it.message == oldMessage) it.copy(message = newMessage) else it
        }
    }

    fun removeMessage(message: String) {
        _messages.value = _messages.value.filter { it.message != message }
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }


    companion object {
        @Volatile
        private var INSTANCE: LocalDb? = null

        fun getInstance(): LocalDb {
            return INSTANCE ?: synchronized(this) {
                val instance = LocalDb()
                INSTANCE = instance
                instance
            }
        }
    }
}
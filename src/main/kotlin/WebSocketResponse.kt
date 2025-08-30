package com.aiyu

import kotlinx.serialization.Serializable


enum class Commands(
) {
    MESSAGE,
    OBSERVE,
    STOP,
}

@Serializable
enum class WebSocketEvents {
    INITIAL,LOADING, UPDATED, DISCONNECTED
}

@Serializable
data class WebSocketResponse<T>(
    val event: WebSocketEvents,
    val message: String? = null,
    val data: T? = null
)
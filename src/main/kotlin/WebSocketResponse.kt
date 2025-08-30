package com.aiyu

import kotlinx.serialization.Serializable


@Serializable
enum class WebSocketEvents {
    INITIAL, UPDATED, DISCONNECTED
}

@Serializable
data class WebSocketResponse<T>(
    val event: WebSocketEvents,
    val message: String? = null,
    val data: T? = null
)
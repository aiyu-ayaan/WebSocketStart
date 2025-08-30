package com.aiyu

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    val localDb = LocalDb.getInstance()
    val connectionManager = ConnectionManager(localDb)
    routing {
        webSocket("/ws/chat") {
            val userId = call.request.queryParameters["userId"]
            if (userId.isNullOrBlank()) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No userId provided"))
                return@webSocket
            }
            connectionManager.addConnection(userId, this)
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        localDb.addMessage("User $userId says: $text")
                        send("Message send acknowledged")
                    }
                }
            } catch (e: Exception) {
                close(
                    CloseReason(
                        CloseReason.Codes.INTERNAL_ERROR,
                        "Error: ${e.localizedMessage}"
                    )
                )
            } finally {
                ConnectionManager(localDb).removeConnection(userId)
                println("Connection closed for user $userId")
            }
        }
    }
}

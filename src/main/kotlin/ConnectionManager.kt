package com.aiyu

import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class ConnectionManager(
    private val localDb: LocalDb
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val connections = ConcurrentHashMap<String, WebSocketSession>()
    private val userWatchJobs = ConcurrentHashMap<String, Job>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun addConnection(
        userId: String,
        session: WebSocketSession
    ) {
        connections[userId] = session
        println("User $userId connected. Total connections: ${connections.size}")
        session.send(
            Frame.Text(
                json.encodeToString(
                    WebSocketResponse(
                        event = WebSocketEvents.INITIAL,
                        data = emptyList<Message>(),
                        message = "Welcome! Connected to WebSocket. ${connections.size} users online."
                    )
                )
            )
        )
        observeMessages(userId)
    }

    fun observeMessages(userId: String) {
        if (userWatchJobs.containsKey(userId)) return // Already observing

        val job = scope.launch {
            localDb.message
                .flowOn(Dispatchers.IO)
                .collect { messages ->
                    connections[userId]?.send(
                        Frame.Text(
                            json.encodeToString(
                                WebSocketResponse(
                                    event = WebSocketEvents.UPDATED,
                                    data = messages,
                                    message = "Messages updated. Total messages: ${messages.size}"
                                )
                            )
                        )
                    )
                }
        }
        userWatchJobs[userId] = job
    }

    suspend fun removeConnection(userId: String) {
        connections.remove(userId)
        userWatchJobs[userId]?.cancel()
        userWatchJobs.remove(userId)
        println("User $userId disconnected. Total connections: ${connections.size}")
    }

}
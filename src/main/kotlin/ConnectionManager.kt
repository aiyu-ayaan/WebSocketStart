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
    private val isObserving = ConcurrentHashMap<String, Boolean>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun addConnection(
        userName: String, session: WebSocketSession
    ) {
        connections[userName] = session
        println("User $userName connected. Total connections: ${connections.size}")
        session.send(
            Frame.Text(
                json.encodeToString(
                    WebSocketResponse(
                        event = WebSocketEvents.INITIAL,
                        data = emptyList<Message>(),
                        message = "Welcome $userName! Connected to WebSocket. ${connections.size} users online."
                    )
                )
            )
        )
    }

    fun updateObservingStatus(userId: String, observing: Boolean) {
        isObserving[userId] = observing
        if (observing) {
            scope.launch {
                observeMessages(userId)
            }
        } else {
            userWatchJobs[userId]?.cancel()
            userWatchJobs.remove(userId)
            isObserving.remove(userId)  // Clean up observing status
        }
    }

    private fun observeMessages(userId: String) {
        if (userWatchJobs.containsKey(userId)) return

        val job = scope.launch {
            localDb.message.flowOn(Dispatchers.IO).collect { messages ->
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
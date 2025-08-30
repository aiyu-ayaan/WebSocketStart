package com.aiyu

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
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
    val geminiHelper = GeminiHelper.getInstance()
    val json = Json { ignoreUnknownKeys = true }
    routing {
        webSocket("/ws/chat") {
            val userName = call.request.queryParameters["userName"]
            if (userName.isNullOrBlank()) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No userId provided"))
                return@webSocket
            }
            connectionManager.addConnection(userName, this)
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val receivedTest = frame.readText()
                        val value: Commands = try {
                            Commands.valueOf(receivedTest.replace(":", ""))
                        } catch (_: Exception) {
                            Commands.MESSAGE
                        }
                        println(value)
                        when (value) {
                            Commands.MESSAGE -> {
                                val prompt = frame.readText().substringAfter("MESSAGE:")
                                localDb.addMessage(
                                    Message(
                                        message = prompt, role = Role.USER
                                    )
                                )

                                send(
                                    Frame.Text(
                                        json.encodeToString(
                                            WebSocketResponse(
                                                event = WebSocketEvents.LOADING,
                                                data = null,
                                                message = "AI is thinking..."
                                            )
                                        )
                                    )
                                )
                                val aiResponse =
                                    geminiHelper.generateResponse(prompt) ?: "Sorry, I couldn't generate a response."
                                localDb.addMessage(
                                    Message(
                                        message = aiResponse, role = Role.AI
                                    )
                                )
                                send(
                                    Frame.Text(
                                        json.encodeToString(
                                            WebSocketResponse(
                                                event = WebSocketEvents.UPDATED,
                                                data = aiResponse,
                                            )
                                        )
                                    )
                                )
                            }

                            Commands.OBSERVE -> {
                                connectionManager.updateObservingStatus(userName, true)
                            }

                            Commands.STOP -> {
                                connectionManager.updateObservingStatus(userName, false)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                close(
                    CloseReason(
                        CloseReason.Codes.INTERNAL_ERROR, "Error: ${e.localizedMessage}"
                    )
                )
            } finally {
                ConnectionManager(localDb).removeConnection(userName)
                println("Connection closed for user $userName")
            }
        }
    }
}

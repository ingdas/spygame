import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.collections.set

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.main() {
    val gameState = GameState()
    val Json = Json { encodeDefaults = true }

    install(WebSockets)
    install(ContentNegotiation) {
        json()
    }
    install(CORS) {
        anyHost()
    }


    routing {
        webSocket("/socket/play") {
            try {
                // Wait for the first message which should be the player's name
                val nameFrame = incoming.receive()
                if (nameFrame is Frame.Text) {
                    val playerName = nameFrame.readText()

                    gameState.newPlayer(playerName, this)

                    // Handle subsequent messages
                    try {
                        incoming.consumeEach { frame ->
                            if (frame is Frame.Text) {
                                // Handle other game messages here
                            }
                        }
                    } finally {
                        // Remove player when they disconnect
                        gameState.removePlayer(this)
                    }
                }
            } catch (e: Exception) {
                println("Error in websocket connection: ${e.message}")
            }
        }
        webSocket("/socket/admin") {
            try {
                gameState.admins.add(this)
                gameState.updatePlayers()
                try {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            val gameInput: GameInput = Json.decodeFromString(frame.readText())
                            println("Starting Game $gameInput")
                            val fulList = (gameInput.subjects + gameInput.ommittedSubject)


                            val connectedPlayers = gameState.connectedPlayers
                            val sessions = connectedPlayers.keys
                            val names = connectedPlayers.values.toList()

                            val spy = sessions.random()
                            spy.send(
                                Json.encodeToString(
                                    PlayerState(
                                        isSpy = true,
                                        hint = gameInput.hint,
                                        subject = "",
                                        theme = "",
                                        subjectList = emptyList(),
                                        allPlayers = names.shuffled()
                                    )
                                )
                            )

                            for ((session, subject) in (sessions - spy).zip(gameInput.subjects.shuffled())) {
                                session.send(
                                    Json.encodeToString(
                                        PlayerState(
                                            isSpy = false,
                                            subject = subject,
                                            theme = gameInput.theme,
                                            subjectList = fulList.shuffled() - subject,
                                            hint = "",
                                            allPlayers = names.shuffled()
                                        )
                                    )
                                )
                            }
                            send("Game Started")
                        }
                    }
                } finally {
                    // Remove player when they disconnect
                    gameState.admins.remove(this)
                }
            } catch (e: Exception) {
                println("Error in websocket connection: ${e.message}")
            }
        }

        staticResources("/", "static")
    }
}


class GameState {
    suspend fun newPlayer(playerName: String, session: DefaultWebSocketServerSession) {
        // Add player to connected players list
        connectedPlayers[session] = playerName

        // Broadcast updated player list to all connected clients
        updatePlayers()

    }

    suspend fun updatePlayers() {
        val message = ConnectedPlayers(connectedPlayers = connectedPlayers.values.toList())
        val playerListJson = Json.encodeToJsonElement(message).toString()

        (connectedPlayers.keys + admins).forEach { conn ->
            conn.outgoing.send(Frame.Text(playerListJson))
        }
    }

    suspend fun removePlayer(session: DefaultWebSocketServerSession) {
        connectedPlayers.remove(session)
        updatePlayers()
    }

    val connectedPlayers: MutableMap<DefaultWebSocketServerSession, String> = mutableMapOf()
    val admins: MutableSet<DefaultWebSocketServerSession> = mutableSetOf()
}

@Serializable
data class GameInput(
    val messageType: String = "GameInput",
    val theme: String,
    val subjects: List<String>,
    val ommittedSubject: String,
    val hint: String
)

@Serializable
data class ConnectedPlayers(
    val messageType: String = "ConnectedPlayers",
    val connectedPlayers: List<String>
)

@Serializable
data class PlayerState(
    val messageType: String = "PlayerState",
    val isSpy: Boolean,
    val theme: String,
    val subject: String,
    val subjectList: List<String>,
    val hint: String,
    val allPlayers: List<String>
)
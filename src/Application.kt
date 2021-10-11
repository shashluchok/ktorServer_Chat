package com.example

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.websocket.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.LinkedHashSet

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(WebSockets)
    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }
    }
    routing {
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        webSocket("/chat{login}") {
            println("Новый пользователь ${call.parameters["login"]}")

            val thisConnection = Connection(this, call.parameters["login"])
            connections += thisConnection
            try {
                val greetingsMessage = "Вы вошли в чат. Число пользователей онлайн: ${connections.count()}"
                send(Gson().toJson(UserWithMessage(user = "Admin", message = greetingsMessage)))
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    val userWithMessage = Gson().toJson(UserWithMessage(user = thisConnection.name, message = receivedText))
                    connections.forEach {
                        it.session.send(userWithMessage)
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                println("Пользователь $thisConnection вышел")
                connections -= thisConnection
            }
        }
    }

}

class Connection(val session: DefaultWebSocketSession, userName:String?) {
    companion object {
        var lastId = AtomicInteger(0)
    }
    val name = if(userName.isNullOrEmpty()) "user${lastId.getAndIncrement()}" else userName
}

data class UserWithMessage (
    val user:String,
    val message:String
        )

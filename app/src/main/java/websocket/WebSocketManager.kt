package websocket

import okhttp3.*
import java.util.concurrent.TimeUnit

object WebSocketManager {
    private const val CONNECTION_ATTEMPTS_MAX = 5
    private const val CONNECTION_INTERVAL_MILLIS = 5000
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var request: Request
    private lateinit var messageListener: MessageListener
    private lateinit var webSocket: WebSocket
    private var isConnected = false
    private var connectionAttempts = 0

    fun initialize(url: String, messageListener: MessageListener) {
        println("Init called")
        okHttpClient = OkHttpClient.Builder()
            .writeTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()
        request = Request.Builder().url(url).build()
        this.messageListener = messageListener
    }

    fun connect() {
        if(isConnected)
            return
        okHttpClient.newWebSocket(request, createWebSocketListener())
    }

    fun reconnect() {
        if(connectionAttempts < CONNECTION_ATTEMPTS_MAX) {
            try {
                Thread.sleep(CONNECTION_INTERVAL_MILLIS.toLong())
                connect()
                connectionAttempts++
            }
            catch(e: InterruptedException) {
                e.printStackTrace()
            }
        }
        else {
            println("$CONNECTION_ATTEMPTS_MAX attempts exceeded.")
        }
    }

    fun sendMessage(message: String): Boolean {
        return if (!isConnected) false else webSocket.send(message)
    }

    fun close() {
//        if(isConnected) {
            webSocket.cancel()
            webSocket.close(1000, "Web socket closed by client.")
//        }
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(socket: WebSocket, response: Response?) {
                super.onOpen(socket, response)
                webSocket = socket
                isConnected = response?.code() == 101
                if(!isConnected) {
                    reconnect()
                }
                else {
                    connectionAttempts = 0
                    messageListener.onConnectSuccess()
                }
            }

            override fun onMessage(webSocket: WebSocket?, message: String?) {
                super.onMessage(webSocket, message)
                messageListener.onMessage(message)
            }

            override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
                isConnected = false
                messageListener.onClose()
                super.onClosing(webSocket, code, reason)
            }

            override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
                isConnected = false
                messageListener.onClose()
                super.onClosed(webSocket, code, reason)
            }

            override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
                super.onFailure(webSocket, t, response)
                if(response != null) {
                    println("Connection failed: ${response.message()}")
                }
                println("Connect throwable: ${t?.message}")
                isConnected = false
                messageListener.onConnectFailure()
                reconnect()
            }
        }
    }
}
package websocket

interface MessageListener {
    fun onConnectSuccess()
    fun onConnectFailure()
    fun onClose()
    fun onMessage(text: String?)
}
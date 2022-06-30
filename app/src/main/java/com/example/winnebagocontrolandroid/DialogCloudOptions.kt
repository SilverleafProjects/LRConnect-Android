package com.example.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import preferences.Preferences
import websocket.MessageListener
import websocket.WebSocketManager
import kotlin.concurrent.thread

class DialogCloudOptions(activity: Activity, ip: String): Dialog(activity), MessageListener {
    private val PORT = 8092
    private lateinit var buttonDialogCloudOptionsToggle: Button
    private lateinit var buttonDialogCloudOptionsDone: Button
    private lateinit var textViewDialogCloudOptionsState: TextView
    var preferences = Preferences(activity.baseContext)
    private var cloudEnabled = false
    private var ipIsValid = false
    private var isCheckingForCloudStatus = false
    private var url: String

    init {
        if(ip != "") {
            ipIsValid = true
            isCheckingForCloudStatus = true
        }
        val baseIP = ip.replace("/", "")
        this.url = "ws://$baseIP:$PORT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_cloud_options)
        println("IP: $url")
        bindUI()

        if(ipIsValid) {
            CoroutineScope(Dispatchers.IO).launch {
                var counter = 0
                connectWebSocket()
                while(isCheckingForCloudStatus) {
                    println("Connecting: $isCheckingForCloudStatus")
                    setConnectingMessage(counter)
                    counter++
                    delay(800)
                }
            }
        }
    }

    override fun cancel() {
        isCheckingForCloudStatus = false
        super.cancel()
    }

    private fun setConnectingMessage(counter: Int) {
        val baseText ="Checking State"
        if(counter % 4 == 0)
            textViewDialogCloudOptionsState.text = "$baseText"
        else if(counter % 4 == 1)
            textViewDialogCloudOptionsState.text = "$baseText ."
        else if(counter % 4 == 2)
            textViewDialogCloudOptionsState.text = "$baseText . ."
        else
            textViewDialogCloudOptionsState.text = "$baseText . . ."

        if(counter > 25) {
            textViewDialogCloudOptionsState.text = "Unable to retrieve cloud status."
            isCheckingForCloudStatus = false
        }
    }

    private fun connectWebSocket() {
        WebSocketManager.initialize(this.url, this)
        WebSocketManager.connect()
        WebSocketManager.sendMessage("LR_EC2ENABLE")
    }

    private fun disconnectWebSocket() {
        WebSocketManager.close()
    }

    private fun cloudOptionToggleState() {
        val state = if(cloudEnabled) "disabled" else "enabled"
        WebSocketManager.sendMessage("LR_EC2ENABLE=$state")
    }

    private fun setButtonToggleText(enabled: String) {
        buttonDialogCloudOptionsToggle.text = enabled
    }

    private fun setTextViewStateText(enabled: String) {
        textViewDialogCloudOptionsState.text = enabled
    }

    private fun bindUI() {
        textViewDialogCloudOptionsState = findViewById(R.id.textViewDialogCloudOptionsState)
//        val state = if(ipIsValid) "Checking State . . ." else "No device found"
        if(!ipIsValid)
            textViewDialogCloudOptionsState.text = "No device found"//state //context.getString(R.string.button_dialog_cloud_options_enable)

        buttonDialogCloudOptionsToggle = findViewById(R.id.buttonDialogCloudOptionsToggle)
        buttonDialogCloudOptionsToggle.text = context.getString(R.string.button_dialog_cloud_options_disable)
        buttonDialogCloudOptionsToggle.setOnClickListener {
            cloudOptionToggleState()
        }

        if(!ipIsValid)
            buttonDialogCloudOptionsToggle.visibility = View.GONE

        buttonDialogCloudOptionsDone = findViewById(R.id.buttonDialogCloudOptionsDone)
        buttonDialogCloudOptionsDone.setOnClickListener {
            if(ipIsValid) {
                CoroutineScope(Dispatchers.IO).launch {
                    disconnectWebSocket()
                }
            }
            this.cancel()
        }
    }

    override fun onConnectSuccess() {
        println("Connected to WebSocket")
        WebSocketManager.sendMessage("LR_EC2ENABLE")
    }

    override fun onConnectFailure() {
        println("WebSocket Connect Failure")
    }

    override fun onClose() {
        println("WebSocket Closed")
        WebSocketManager.close()
    }

    override fun onMessage(message: String?) {
        val messageParts = message!!.split("=")
        if(messageParts[0] == "LR_EC2ENABLE") {
            println("LR_EC2ENABLE: ${messageParts[1]}")
            if(messageParts[1] == "enabled") {
                cloudEnabled = true
                setButtonToggleText("disable")
                setTextViewStateText("Enabled")
            }
            else {
                cloudEnabled = false
                setButtonToggleText("enable")
                setTextViewStateText("Disabled")
            }
            isCheckingForCloudStatus = false
        }
    }
}
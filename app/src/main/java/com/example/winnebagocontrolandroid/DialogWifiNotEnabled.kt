package com.example.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.Window
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class DialogWifiNotEnabled(activity: Activity, webView: WebView): Dialog(activity) {
    private lateinit var buttonDialogWifiNotConnectedGoToCloudSite: Button
    private lateinit var buttonDialogWifiNotConnectedExitApp: Button
    private lateinit var textViewDialogWifiNotConnectedMessage: TextView
    private lateinit var textViewDialogWifiNotConnectedError: TextView

    private var webView: WebView
    private var activity: Activity
    init {
        setCancelable(false)
        webView.also{ this.webView = it }
        activity.also { this.activity = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_wifi_not_enabled)

        bindUI()

        CoroutineScope(Dispatchers.Default).launch {
            checkForWifiConnection()
        }
    }

    private suspend fun checkForWifiConnection() {
        val wifiManager = (context.getSystemService(Context.WIFI_SERVICE) as WifiManager)
        while(wifiManager.wifiState != WifiManager.WIFI_STATE_ENABLED) {
            delay(1000)
            println("Call from dialog loop.")
        }
        delay(1500)
        MainActivity.callScanNetworkOnDialogClose = true
        this.cancel()
    }

    private fun bindUI() {
        buttonDialogWifiNotConnectedGoToCloudSite = findViewById(R.id.buttonDialogWifiNotConnectedGoToCloudSite)
        buttonDialogWifiNotConnectedGoToCloudSite.setOnClickListener {
            webView.post(Runnable {
                webView.loadUrl(activity.resources.getString(R.string.url_cloud))
            })
            this.cancel()
        }

        buttonDialogWifiNotConnectedExitApp = findViewById(R.id.buttonDialogWifiNotConnectedExitApp)
        buttonDialogWifiNotConnectedExitApp.setOnClickListener{
            activity.finish()
            exitProcess(0)
        }

        textViewDialogWifiNotConnectedMessage = findViewById(R.id.textViewDialogWifiNotConnectedMessage)
        textViewDialogWifiNotConnectedMessage.text = activity.resources.getString(R.string.text_view_dialog_wifi_not_connected_message)

        textViewDialogWifiNotConnectedError = findViewById(R.id.textViewDialogWifiNotConnectedError)
        textViewDialogWifiNotConnectedError.text = activity.resources.getString(R.string.text_view_dialog_wifi_not_connected_error)
    }
}
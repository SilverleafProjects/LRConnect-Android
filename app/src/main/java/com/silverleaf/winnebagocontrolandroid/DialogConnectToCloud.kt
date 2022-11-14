package com.silverleaf.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.Window
import android.webkit.WebView
import android.widget.Button

class DialogConnectToCloud(activity: Activity, webView: WebView): Dialog(activity) {
    private lateinit var buttonDialogConnectToCloudConnect: Button
    private lateinit var buttonDialogConnectToCloudCancel: Button

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
        setContentView(R.layout.dialog_connect_to_cloud)

        bindUI()
    }

    override fun cancel() {
        super.cancel()
    }
    private fun bindUI() {
        buttonDialogConnectToCloudConnect = findViewById(R.id.buttonDialogConnectToCloudConnect)
        buttonDialogConnectToCloudConnect.setOnClickListener {
            webView.post(Runnable {
                webView.loadUrl(activity.resources.getString(R.string.url_cloud))
            })
            this.cancel()
        }

        buttonDialogConnectToCloudCancel = findViewById(R.id.buttonDialogConnectToCloudCancel)
        buttonDialogConnectToCloudCancel.setOnClickListener {
            MainActivity.callScanNetworkOnDialogClose = false
            this.cancel()
        }
    }
}
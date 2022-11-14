package com.silverleaf.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.Window
import android.webkit.WebView
import android.widget.Button

class DialogLRNotFound(activity: Activity, webView: WebView): Dialog(activity) {
    private lateinit var buttonDialogLRNotFoundRescan: Button
    private lateinit var buttonDialogLRNotFoundCancel: Button
    private lateinit var buttonDialogLRNotFoundCloud: Button

    private var webView: WebView
    private var activity: Activity
    init {
        webView.also{ this.webView = it }
        activity.also { this.activity = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_lr_not_found)

        bindUI()
    }

    private fun bindUI() {
        buttonDialogLRNotFoundRescan = findViewById(R.id.buttonDialogLRNotFoundRescan)
        buttonDialogLRNotFoundRescan.setOnClickListener {
            MainActivity.callScanNetworkOnDialogClose = true
            this.cancel()
        }

        buttonDialogLRNotFoundCancel = findViewById(R.id.buttonDialogLRNotFoundCancel)
        buttonDialogLRNotFoundCancel.setOnClickListener {
            MainActivity.callScanNetworkOnDialogClose = false
            this.cancel()
        }

        buttonDialogLRNotFoundCloud = findViewById(R.id.lrNotFoundGoToCloud)
        buttonDialogLRNotFoundCloud.setOnClickListener {
            MainActivity.callScanNetworkOnDialogClose = false
            webView.post(Runnable {
                webView.loadUrl(activity.resources.getString(R.string.url_cloud))
            })
            this.cancel()
        }
    }
}
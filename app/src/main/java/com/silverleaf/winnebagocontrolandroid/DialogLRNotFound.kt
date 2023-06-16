package com.silverleaf.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.ipAddress
import webviewsettings.setWebView

private fun callScanForLR125(activity: MainActivity) = activity.scanNetwork()

class DialogLRNotFound(activity: Activity, webView: WebView): Dialog(activity) {
    private lateinit var buttonDialogLRNotFoundRescan: Button
    private lateinit var buttonDialogLRNotFoundCloud: Button
    private lateinit var buttonCancel: Button
    private lateinit var textViewNoInternet: TextView

    private var webView: WebView
    private var activity: Activity
    init {
        setCancelable(false);
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
        textViewNoInternet = findViewById((R.id.textViewNoInternet))
        textViewNoInternet.visibility = View.INVISIBLE

        buttonDialogLRNotFoundRescan = findViewById(R.id.buttonDialogLRNotFoundRescan)
        buttonDialogLRNotFoundRescan.setOnClickListener {
            try {
                MainActivity.LRConnectCalledFromSettingsMenu = true
                callScanForLR125(MainActivity())
            }catch(e:Exception){
                e.printStackTrace()
            }
            if(ipAddress != null) {
                webView.post(Runnable {
                    var address = ipAddress.replace("/", "")
                    webView.loadUrl("$address")
                })
                MainActivity.LRConnectCalledFromSettingsMenu = false
                this.cancel()
            }else{
                MainActivity.LRConnectCalledFromSettingsMenu = true
                callScanForLR125(MainActivity())
            }

        }
        buttonDialogLRNotFoundCloud = findViewById(R.id.lrNotFoundGoToCloud)
        buttonDialogLRNotFoundCloud.setOnClickListener {
            MainActivity.callScanNetworkOnDialogClose = false
            if(MainActivity.internetAvailable) {
                textViewNoInternet.visibility = View.INVISIBLE
                webView.post(Runnable {
                    webView.loadUrl(activity.resources.getString(R.string.url_cloud))
                })
                this.cancel()
            } else {
                textViewNoInternet.visibility = View.VISIBLE
            }

        }

        buttonCancel = findViewById(R.id.returnButton)
        buttonCancel.setOnClickListener{
            this.cancel()
        }

    }
}
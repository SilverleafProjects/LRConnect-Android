package com.silverleaf.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import com.silverleaf.lrgizmo.R

class DialogConnectToCloud(activity: MainActivity, webView: WebView): Dialog(activity) {
    private lateinit var buttonDialogConnectToCloudConnect: Button
    private lateinit var buttonDialogConnectToCloudCancel: Button
    private lateinit var textViewNoInternet: TextView

    private var webView: WebView
    private var activity: MainActivity
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
        textViewNoInternet = findViewById((R.id.textViewNoInternet2))
        textViewNoInternet.visibility = View.INVISIBLE

        buttonDialogConnectToCloudConnect = findViewById(R.id.buttonDialogConnectToCloudConnect)
        buttonDialogConnectToCloudConnect.setOnClickListener {
         //   if(MainActivity.internetAvailable) {
                textViewNoInternet.visibility = View.INVISIBLE
                activity.navigateToCloud();
                this.cancel()
            /*
            } else {
                textViewNoInternet.visibility = View.VISIBLE
            }
        */
        }

        /*buttonDialogConnectToCloudCancel = findViewById(R.id.buttonDialogConnectToCloudCancel)
        buttonDialogConnectToCloudCancel.setOnClickListener {
            MainActivity.callScanNetworkOnDialogClose = false
            this.cancel()
        }*/
    }
}
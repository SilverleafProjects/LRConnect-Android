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


class DialogAppUsesLocation(activity: Activity, webView: WebView): Dialog(activity) {

    private lateinit var buttonDialogDismiss: Button

    private var webView: WebView
    private var activity: Activity

    init {
        setCancelable(false)
        webView.also{this.webView = it}
        activity.also{this.activity = it}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_app_uses_location)

        bindUI()
    }

    private fun bindUI() {

        buttonDialogDismiss = findViewById(R.id.acceptAndContinue)
        buttonDialogDismiss.setOnClickListener {
            MainActivity.preferences.saveBoolean("didUserAcceptData", true)
            this.cancel()
        }

    }

}
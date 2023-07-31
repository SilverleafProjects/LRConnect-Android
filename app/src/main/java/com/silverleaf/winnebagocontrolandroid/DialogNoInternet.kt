package com.silverleaf.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.Window
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import com.silverleaf.lrgizmo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class DialogNoInternet(activity: Activity): Dialog(activity) {
    private lateinit var buttonDialogDismiss: Button

    private var activity: Activity
    init {
        setCancelable(false)
        activity.also { this.activity = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_no_internet)

        bindUI()
    }

    private fun bindUI() {

        buttonDialogDismiss = findViewById(R.id.buttonDialogNoInternet)
        buttonDialogDismiss.setOnClickListener {
            this.cancel()
        }

    }
}

class DialogNoCloudService(activity: Activity): Dialog(activity){

    private lateinit var buttonDialogDismiss: Button

    private var activity: Activity
    init {
        setCancelable(false)
        activity.also { this.activity = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_no_cloud_service)




        bindUI()
    }

    private fun bindUI() {

        buttonDialogDismiss = findViewById(R.id.buttonDialogNoCloudService)
        buttonDialogDismiss.setOnClickListener {
            this.cancel()
        }

    }

}
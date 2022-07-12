package com.example.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.Window
import android.widget.Button

class DialogLRNotFound(activity: Activity): Dialog(activity) {
    private lateinit var buttonDialogLRNotFoundRescan: Button
    private lateinit var buttonDialogLRNotFoundCancel: Button

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
    }
}
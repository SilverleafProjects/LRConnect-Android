package com.silverleaf.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Window
import android.widget.TextView
import com.silverleaf.lrgizmo.R
import kotlinx.coroutines.*

class DialogNetworkScanInProgress(activity: Activity): Dialog(activity) {
    private lateinit var textViewDialogNetworkScanInProgressMessage: TextView


    private var activity: Activity
    private var dialogNotCancelled = true
    init {
        setCancelable(false)
        activity.also { this.activity = it }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_network_scan_in_progress)
        this.setCanceledOnTouchOutside(true)

        bindUI()

        CoroutineScope(Dispatchers.IO).launch {
            var counter = 0
            while(dialogNotCancelled) {
                if(MainActivity.isConnectedToLR) {
                    closeDialog()
                }
                if(MainActivity.isConnectedToCloud){
                    closeDialog()
                }
                counter++
                setMessageText(counter)
                delay(600)
            }
        }
    }


    override fun cancel() {
        this.dialogNotCancelled = false
        super.cancel()
    }
    private fun closeDialog() {
        this.dialogNotCancelled = false
        super.cancel()
    }

    private fun setMessageText(counter: Int) {
        val baseText = activity.resources.getText(R.string.text_view_dialog_network_scan_in_progress_message)
        var dots = counter % 4
        var messageText = baseText
        while(dots > 0) {
            messageText = "$messageText ."
            dots--
        }

        textViewDialogNetworkScanInProgressMessage.text = messageText

    }

    private fun bindUI() {
        textViewDialogNetworkScanInProgressMessage = findViewById(R.id.textViewDialogNetworkScanInProgressMessage)
        textViewDialogNetworkScanInProgressMessage.text =  activity.resources.getText(R.string.text_view_dialog_network_scan_in_progress_message)
    }
}
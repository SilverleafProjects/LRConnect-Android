package com.example.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.Window
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView

class DialogGenericError(activity: Activity, webView: WebView, displayError: String, displayOption: String): Dialog(activity) {
    private lateinit var buttonDialogGenericErrorAccept: Button
    private lateinit var buttonDialogGenericErrorCancel: Button
    private lateinit var textViewDialogGenericErrorDisplayError: TextView
    private lateinit var textViewDialogGenericErrorDisplayOption: TextView

//    companion object {
//        var displayError: String = ""
//        var displayOption: String = ""
//        fun executeOptionAccept() { }
//    }

    private var webView: WebView
    private val displayError: String
    private val displayOption: String
    init {
        setCancelable(false)
        webView.also{ this.webView = it }
        this.displayError = displayError
        this.displayOption = displayOption
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_generic_error)

        bindUI()
    }

    private fun bindUI() {
        buttonDialogGenericErrorAccept = findViewById(R.id.buttonDialogGenericErrorAccept)

        buttonDialogGenericErrorCancel = findViewById(R.id.buttonDialogGenericErrorCancel)
        buttonDialogGenericErrorCancel.setOnClickListener{
            this.cancel()
        }

        textViewDialogGenericErrorDisplayError = findViewById(R.id.textViewDialogGenericErrorDisplayError)
        textViewDialogGenericErrorDisplayError.text = this.displayError

        textViewDialogGenericErrorDisplayOption = findViewById(R.id.textViewDialogGenericErrorDisplayOption)
        textViewDialogGenericErrorDisplayOption.text = this.displayOption
    }
}
package com.silverleaf.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.Window
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import preferences.Preferences
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DialogEnterIPAddress(activity: Activity, webView: WebView): Dialog(activity) {
    private lateinit var buttonDialogEnterIPAddressAccept: Button
    private lateinit var buttonDialogEnterIPAddressCancel: Button
    private lateinit var editTextDialogEnterIPAddress: EditText
    private lateinit var textViewDialogEnterIPAddressError: TextView
    private var preferences = Preferences(activity.baseContext)
    private lateinit var firstByte: String
    private lateinit var secondByte: String

    private var webView: WebView
    init {
        setCancelable(false)
        webView.also{ this.webView = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_enter_ip_address)

        bindUI()
    }

    private fun loadURL() {
        val firstInputByte = editTextDialogEnterIPAddress.text.toString().split(".")[0]
        val secondInputByte = editTextDialogEnterIPAddress.text.toString().split(".")[1]

        if(firstInputByte != firstByte) {
            textViewDialogEnterIPAddressError.text = context.resources.getString(R.string.ip_address_not_local)
            return
        }

        if(secondInputByte != secondByte) {
            textViewDialogEnterIPAddressError.text = context.resources.getString(R.string.ip_address_not_local)
            return
        }

        val ipAddress = editTextDialogEnterIPAddress.text!!.toString()
        preferences.saveString("IP", ipAddress)

        MainActivity.ipAddress = ipAddress

        this.cancel()

        webView.post(Runnable {
            val protocol = context.resources.getString(R.string.protocol_http)
            webView.loadUrl("$protocol$ipAddress")
        })
    }

    private fun bindUI() {
        buttonDialogEnterIPAddressAccept = findViewById(R.id.buttonDialogEnterIPAddressAccept)
        buttonDialogEnterIPAddressAccept.setOnClickListener {
            textViewDialogEnterIPAddressError.text = ""
            this.loadURL()
        }

        buttonDialogEnterIPAddressCancel = findViewById(R.id.buttonDialogEnterIPAddressCancel)
        buttonDialogEnterIPAddressCancel.setOnClickListener {
            this.cancel()
        }

        textViewDialogEnterIPAddressError = findViewById(R.id.textViewDialogEnterIPAddressError)

        editTextDialogEnterIPAddress = findViewById(R.id.editTextDialogEnterIPAddress)
        editTextDialogEnterIPAddress.addTextChangedListener {
            textViewDialogEnterIPAddressError.text = ""
        }

        val ipAddress =  this.getLocalIPAddress()
        firstByte = ipAddress!!.split(".")[0]
        secondByte = ipAddress!!.split(".")[1]

        val url = "$firstByte.$secondByte."
        editTextDialogEnterIPAddress.setText(url)
    }

    @Throws(UnknownHostException::class)
    private fun getLocalIPAddress(): String? {
        val wifiManager = (context.getSystemService(WIFI_SERVICE) as WifiManager)
        val wifiInfo = wifiManager.connectionInfo
        val ipInt = wifiInfo.ipAddress
        return InetAddress.getByAddress(
            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()
        ).hostAddress
    }
}
package com.silverleaf.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import com.silverleaf.lrgizmo.R
import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.NSDListener
import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.NSDManager
import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.callScanNetworkOnDialogClose

import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.ipAddress
import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.lr125DataStorage
import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.noDetectedLROnNetwork
import kotlinx.coroutines.CoroutineScope
import java.net.DatagramPacket
import java.net.SocketTimeoutException


private fun convertByteArray(byteArray: ByteArray): String {
    var returnString: String = ""
    for (byte in byteArray) returnString += byte.toInt().toChar()
    return returnString
}
private fun compareTimeStamps(savedDate: Long, currentDate: Long): Boolean {
    return (currentDate - savedDate) < 60
}
private fun isValidSilverLeafDevice(messageString: String): Boolean {
    return (messageString.contains("LR125")) || (messageString.contains("RVHALO"))
}
private fun scanForLR125(){
    MainActivity.udpListenerSocket.soTimeout = 5000
    try{
        val buffer = ByteArray(4096)
        val timestamp = System.currentTimeMillis() / 1000
        val udpPacket = DatagramPacket(buffer, buffer.size)

        MainActivity.udpListenerSocket.receive(udpPacket)

        var incomingAddress = udpPacket.address
        var incomingMessage = convertByteArray(udpPacket.data)

        var timestampedPacket = Pair(first = timestamp, second = incomingMessage)

        lr125DataStorage.put(incomingAddress, timestampedPacket)
    }catch(e: SocketTimeoutException){
        e.printStackTrace()
    }
}

private fun scanForNSD(){
    NSDManager.discoverServices(
        "_http._tcp",
        NsdManager.PROTOCOL_DNS_SD,
        NSDListener
    )
}

class DialogLRNotFound(activity: MainActivity, webView: WebView): Dialog(activity) {
    private lateinit var buttonDialogLRNotFoundRescan: Button
    private lateinit var buttonDialogLRNotFoundCloud: Button
    private lateinit var buttonCancel: Button
    private lateinit var textViewNoInternet: TextView

    private var webView: WebView
    private var activity: MainActivity

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
            MainActivity.callScanNetworkOnDialogClose = true
            this.cancel()
        }

        buttonDialogLRNotFoundCloud = findViewById(R.id.lrNotFoundGoToCloud)
        buttonDialogLRNotFoundCloud.setOnClickListener {
            MainActivity.callScanNetworkOnDialogClose = false
          //  if(MainActivity.internetAvailable) {
                textViewNoInternet.visibility = View.INVISIBLE
                  activity.navigateToCloud();
                this.cancel()
            /*
            } else {
                MainActivity.goToCloud = false
                textViewNoInternet.visibility = View.VISIBLE
            }
            */
        }

    }
}
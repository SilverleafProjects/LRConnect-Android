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
import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.lrDiscoveryDialog
import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.noDetectedLROnNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import webviewsettings.setWebView
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

class DialogLRNotFound(activity: Activity, webView: WebView): Dialog(activity) {
    private lateinit var buttonDialogLRNotFoundRescan: Button
    private lateinit var buttonDialogLRNotFoundCloud: Button
    private lateinit var buttonCancel: Button
    private lateinit var textViewNoInternet: TextView

    private var webView: WebView
    private var activity: Activity
    var deleteWhenTestingFinished: Boolean = true
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
/*
            try {
                CoroutineScope(Dispatchers.IO).launch{scanForLR125()}
            }catch(e:Exception){
                e.printStackTrace()
            }
            try{
                CoroutineScope(Dispatchers.IO).launch { scanForNSD() }
            }catch(e:Exception){
                e.printStackTrace()
            }
*/
            var checkpoint: Boolean = false
            if(MainActivity.lr125DataStorage.isNotEmpty()) {
                println("Size: ${lr125DataStorage.size}")
                for (entry in MainActivity.lr125DataStorage) {
                    if (compareTimeStamps(
                            entry.value!!.first,
                            (System.currentTimeMillis() / 1000)
                        )
                    ){
                        if (isValidSilverLeafDevice(entry.value!!.second))
                        {
                            MainActivity.ipAddress = entry.key.toString()

                            webView.post(Runnable {
                                var address = ipAddress.replace("/", "")
                                println("IP Address: $address")
                                webView.loadUrl("$address")
                            })
                        }
                    }
                }
                MainActivity.lrDiscoveryDialog = true
                this.cancel()
            }else if(NSDListener.serviceList.isNotEmpty()){

                var firstDetectedLR = NSDListener.serviceList[0]
                    var nsdResolveListener: NsdManager.ResolveListener =
                        object : NsdManager.ResolveListener {
                            override fun onResolveFailed(detectedLR: NsdServiceInfo?, p1: Int) {
                                MainActivity.lrDiscoveryDialog = true
                            }

                            override fun onServiceResolved(detectedLR: NsdServiceInfo?) {
                                if (detectedLR != null) webView.loadUrl(detectedLR.host.toString())
                            }
                        }
                        NSDManager.resolveService(firstDetectedLR, nsdResolveListener)
                        NSDManager.stopServiceDiscovery(NSDListener)

                        MainActivity.failedToDiscoverLR = false
                        this.cancel()

            }else{
                textViewNoInternet.visibility = View.VISIBLE
            }
            /*
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
            }else if(MainActivity.noDetectedLROnNetwork){
                Log.d("Test Point","No Detected LR On Network")
                MainActivity.LRConnectCalledFromSettingsMenu = false
                this.cancel()
            }else{
                MainActivity.LRConnectCalledFromSettingsMenu = true
                callScanForLR125(MainActivity())
            }
*/
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
            callScanNetworkOnDialogClose = false
            lrDiscoveryDialog = false
            this.cancel()
        }

    }
}
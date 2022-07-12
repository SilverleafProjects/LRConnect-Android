package com.example.winnebagocontrolandroid

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import preferences.Preferences
import scannetwork.MdnsHandler
import webviewsettings.setWebView
import java.lang.Runnable

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    lateinit var progressBar: ProgressBar
    var dialogSide: Int = 0
    private var dialogNetworkScanInProgress: DialogNetworkScanInProgress? = null

    companion object {
        var ipAddress: String = ""
        var valueCallBack: ValueCallback<Array<Uri>>? = null
        const val FILE_RESULT_CODE: Int = 69
        var callScanNetworkOnDialogClose: Boolean = false
        lateinit var preferences: Preferences
        fun saveTokenToPreferences(token: String) {
            preferences.saveString("token", token)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferences = Preferences(this)
        if(preferences.retrieveString("token") != null) {
            val token = preferences.retrieveString("token")
            println("Found token: $token")
        }
        dialogSide = 9 *  Math.min(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels) / 10

        bindUI()

        if(!wifiIsEnabled())
            showDialogWifiNotEnabled()
        else
            scanNetwork()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_action_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.main_page-> appendToIPAddress("")
            R.id.scan_network-> scanNetwork()
            R.id.enter_ip_address-> showDialogEnterIPAddress()
//            R.id.clear_preferences-> preferences.clearPreferences()
            R.id.clear_cache->clearBrowserCache()
            R.id.admin_page-> appendToIPAddress(resources.getString(R.string.route_admin))
            R.id.network_page-> appendToIPAddress(resources.getString(R.string.route_network))
            R.id.cloud_options-> showDialogCloudOptions()
            R.id.cloud_page-> navigateToCloud()
            else -> println("default")
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            FILE_RESULT_CODE-> {
                if(RESULT_OK == resultCode) {
                    valueCallBack?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
                }
            }
        }
    }

    private fun setIPAddressToLR125(route: String = "") {
        runOnUiThread {
            dialogNetworkScanInProgress = DialogNetworkScanInProgress(this)
            dialogNetworkScanInProgress?.show()
            dialogNetworkScanInProgress?.window?.setLayout(dialogSide, dialogSide)
        }

        val mdnsHandler = MdnsHandler(this)
        val bonjourServices = mdnsHandler.services
        var failedToFindLR125: Boolean = true
        if (bonjourServices.isNotEmpty()) {
            dialogNetworkScanInProgress?.cancel()
            callScanNetworkOnDialogClose = false

            for(i in 0 until bonjourServices.size) {
                println(bonjourServices[i].inet4Address.toString())
                if(ipAddressIsValid(bonjourServices[i].inet4Address.toString())) {
                    ipAddress = bonjourServices[i].inet4Address.toString()
                    loadURL("$ipAddress/$route")
                    failedToFindLR125 = false
                    println("Loading index $i")
                    break
                }
            }
        }
        if (failedToFindLR125) {
            dialogNetworkScanInProgress?.cancel()
            runOnUiThread {
                showDialogLRNotFound()
            }
        }
    }

    private fun scanNetwork(route: String = "") {
        if(!wifiIsEnabled())
            showDialogWifiNotEnabled()
        else {
            CoroutineScope(Dispatchers.IO).launch {
                setIPAddressToLR125(route)
            }
        }
    }

    private fun loadURL(url: String) {
        webView.post(Runnable {
            val protocol = resources.getString(R.string.protocol_http)
            webView.loadUrl("$protocol$url")
        })
    }

    /***
     * TODO: change R.string.url_cloud
     * R.string.url_cloud currently set to test site. // currently correct.
     */
    private fun navigateToCloud() {
        webView.post(Runnable {
            val urlCloud: String = resources.getString(R.string.url_cloud)
            webView.loadUrl(urlCloud)
        })
    }

    private fun showDialogEnterIPAddress() {
        if(!wifiIsEnabled())
            showDialogWifiNotEnabled()
        else {
            val dialogEnterIPAddress = DialogEnterIPAddress(this, webView)
            dialogEnterIPAddress.show()
            dialogEnterIPAddress.window?.setLayout(dialogSide, dialogSide)
        }
    }

    private fun showDialogCloudOptions() {
        if(!wifiIsEnabled())
            showDialogWifiNotEnabled()
        else {
            val dialogCloudOptions = DialogCloudOptions(this, ipAddress)
            dialogCloudOptions.show()
            dialogCloudOptions.window?.setLayout(dialogSide, dialogSide)
        }
    }

    private fun showDialogWifiNotEnabled() {
        val dialogWifiNotEnabled = DialogWifiNotEnabled(this, webView)
        dialogWifiNotEnabled.show()
        dialogWifiNotEnabled.window?.setLayout(dialogSide, dialogSide)

        dialogWifiNotEnabled.setOnCancelListener {
            if(callScanNetworkOnDialogClose) {
                scanNetwork()
            }
        }
    }

    private fun showDialogLRNotFound() {
        val dialogLRNotFound = DialogLRNotFound(this)
        dialogLRNotFound.show()
        dialogLRNotFound.window?.setLayout(dialogSide, dialogSide)
        dialogLRNotFound.setOnCancelListener {
            if(callScanNetworkOnDialogClose) {
                scanNetwork()
            }
        }
    }

    private fun wifiIsEnabled() : Boolean {
        val wifiManager = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)
        return wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLED
    }

    private fun clearBrowserCache() {
        webView.clearHistory()
        webView.clearFormData()
        webView.clearCache(true)
    }

    private fun appendToIPAddress(route: String) {
        if(ipAddressIsValid(ipAddress)) {
            loadURL("$ipAddress/$route")
            return
        }
        scanNetwork(route)
    }

    private fun ipAddressIsValid(ipToValidate: String): Boolean {
        val ip = ipToValidate.replace("/", "")
        val ipParts = ip.split('.')
        if(ipParts.size != 4)
            return false

        for(i in ipParts.indices) {
            if(ipParts[i].toInt() > 255 || ipParts[i].toInt() < 0)
                return false
        }
        return true
    }

    private fun bindUI() {
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBarWebView)
        progressBar.visibility = View.VISIBLE
        setWebView(webView, progressBar, this)

        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setCustomView(R.layout.action_bar)
        val background : Drawable = resources.getDrawable(R.drawable.action_bar_border)
        supportActionBar?.setBackgroundDrawable(background)
    }
}
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import preferences.Preferences
import scannetwork.MdnsHandler
import webviewsettings.setWebView

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    lateinit var progressBar: ProgressBar
//    lateinit var preferences: Preferences
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
        val wifiManager = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)

        if(wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLED) {
            scanNetwork()
        }

        if(wifiManager.wifiState == WifiManager.WIFI_STATE_DISABLED) {
            showDialogWifiNotEnabled()
        }
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
            R.id.clear_preferences-> preferences.clearPreferences()
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
        if (bonjourServices.isNotEmpty()) {
            dialogNetworkScanInProgress?.cancel()
            callScanNetworkOnDialogClose = false
            ipAddress = bonjourServices[0].inet4Address.toString()
            println(ipAddress)
            loadURL("$ipAddress/$route")
        }
        if (bonjourServices.isEmpty()) {
            dialogNetworkScanInProgress?.cancel()
            runOnUiThread {
                println("No LR125 Found.")
                val dialogGenericError = DialogGenericError(
                    this,
                    webView,
                    "Unable to connect to the LR125",
                    "Rescan for devices?"
                )
                dialogGenericError.show()
                dialogGenericError.window?.setLayout(dialogSide, dialogSide)
                dialogGenericError.setOnCancelListener {
                    if(callScanNetworkOnDialogClose) {
                        scanNetwork()
                    }
                }
            }
        }
    }

    private fun scanNetwork(route: String = "") {
        val wifiManager = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)
        if(wifiManager.wifiState != WifiManager.WIFI_STATE_ENABLED) {
            showDialogWifiNotEnabled()
        }
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
            println("${webView.url}")
        })
    }

    /***
     * TODO: change R.string.url_cloud
     * R.string.url_cloud currently set to test site.
     */
    private fun navigateToCloud() {
        webView.post(Runnable {
            val urlCloud: String = resources.getString(R.string.url_cloud)
            webView.loadUrl(urlCloud)
        })
    }

    private fun showDialogEnterIPAddress() {
        val wifiManager = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)
        if(wifiManager.wifiState != WifiManager.WIFI_STATE_ENABLED) {
            showDialogWifiNotEnabled()
        }
        else {
            val dialogEnterIPAddress = DialogEnterIPAddress(this, webView)
            dialogEnterIPAddress.show()
            dialogEnterIPAddress.window?.setLayout(dialogSide, dialogSide)
        }
    }

    private fun showDialogCloudOptions() {
        val wifiManager = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)
        if(wifiManager.wifiState != WifiManager.WIFI_STATE_ENABLED) {
            showDialogWifiNotEnabled()
        }
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

    private fun wifiNotEnabledGuard() {
        val wifiManager = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)
        if(wifiManager.wifiState != WifiManager.WIFI_STATE_ENABLED) {
            showDialogWifiNotEnabled()
        }
    }

    private fun clearBrowserCache() {
        webView.clearHistory()
        webView.clearFormData()
        webView.clearCache(true)
    }

    private fun appendToIPAddress(route: String) {
        if(ipAddress != "" && ipAddress != "null") {
            loadURL("$ipAddress/$route")
            return
        }
        scanNetwork(route)
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
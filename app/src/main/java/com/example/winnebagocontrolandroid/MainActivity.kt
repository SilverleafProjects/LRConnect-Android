package com.example.winnebagocontrolandroid

import android.graphics.drawable.Drawable
import android.net.wifi.WifiManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.appcompat.app.ActionBar
import preferences.Preferences
import scannetwork.MdnsHandler
import webviewsettings.setWebView

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    lateinit var progressBar: ProgressBar
    lateinit var preferences: Preferences

    companion object {
        var ipAddress: String = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val wifiManager = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)

        if(wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLED)
            println("Wifi enabled")

        if(wifiManager.wifiState == WifiManager.WIFI_STATE_DISABLED)
            println("Wifi disabled")

        bindUI()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_action_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.main_page-> appendToIPAddress("")
            R.id.scan_network-> scanNetwork(webView)
            R.id.enter_ip_address-> showDialogEnterIPAddress()
            R.id.clear_preferences-> preferences.clearPreferences()
            R.id.clear_cache->clearBrowserCache()
            R.id.admin-> appendToIPAddress(resources.getString(R.string.route_admin))
            R.id.network-> appendToIPAddress(resources.getString(R.string.route_network))
            else -> println("default")
        }
        return super.onOptionsItemSelected(item)
    }

    private fun scanNetwork(webView: WebView, route: String = "") {
        Thread(Runnable {
            val mdnsHandler = MdnsHandler(this)
            val bonjourServices = mdnsHandler.services
            if (bonjourServices.isNotEmpty()) {
                ipAddress = bonjourServices[0].inet4Address.toString()
                println(ipAddress)
                this.loadURL("$ipAddress/$route")
            }
            if(bonjourServices.isEmpty()) {
                println("No LR125 Found.")
            }
        }).start()
    }

    private fun loadURL(url: String) {
        webView.post(Runnable {
            val protocol = resources.getString(R.string.protocol)
            webView.loadUrl("$protocol$url")
            println("${webView.url}")
        })
    }

    private fun showDialogEnterIPAddress() {
        val dialogEnterIPAddress = DialogEnterIPAddress(this, webView)
        dialogEnterIPAddress.show()
        val shortest: Int =
            Math.min(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
        val dialogSide: Int =  shortest * 9 / 10
        dialogEnterIPAddress.window?.setLayout(dialogSide, dialogSide)
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
        scanNetwork(webView, route)
    }

    private fun bindUI() {
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBarWebView)
        progressBar.visibility = View.VISIBLE
        setWebView(webView, progressBar)
        preferences = Preferences(this)

        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setCustomView(R.layout.action_bar)
        val background : Drawable = resources.getDrawable(R.drawable.action_bar_border)
        supportActionBar?.setBackgroundDrawable(background)
    }
}
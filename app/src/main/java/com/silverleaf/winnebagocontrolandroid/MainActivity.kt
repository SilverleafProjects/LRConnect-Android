package com.silverleaf.winnebagocontrolandroid

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.*
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.*
import preferences.Preferences
import scannetwork.MdnsHandler
import webviewsettings.setWebView
import java.lang.Runnable
import java.net.InetAddress

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    lateinit var progressBar: ProgressBar
    var dialogSide: Int = 0
    private var dialogNetworkScanInProgress: DialogNetworkScanInProgress? = null
    private var dialogConnectToCloud: DialogConnectToCloud? = null
    private var dialogWifiNotEnabled: DialogWifiNotEnabled? = null
    private var dialogLRNotFound: DialogLRNotFound? = null
    private var lastNetwork: Network? = null
    private val lifecycleListener: LifecycleListener by lazy {
        LifecycleListener(webView)
    }

    companion object {
        var ipAddress: String = ""
        var valueCallBack: ValueCallback<Array<Uri>>? = null
        const val FILE_RESULT_CODE: Int = 69
        const val PERMISSION_CODE_ACCEPTED = 1
        const val PERMISSION_CODE_NOT_AVAILABLE = 0
        var callScanNetworkOnDialogClose: Boolean = false
        var internetAvailable: Boolean = false
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
        requestLocationPermission()
        setNetworkChangeCallBack()

        setupLifecycleListener()
    }

    private fun setupLifecycleListener() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_action_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.main_page-> appendToIPAddress("")
            R.id.refresh-> refreshPage()
            R.id.scan_network-> scanNetwork()
            R.id.enter_ip_address-> showDialogEnterIPAddress()
//            R.id.clear_preferences-> preferences.clearPreferences()
            R.id.clear_cache->clearBrowserCache()
            R.id.admin_page-> appendToIPAddress(resources.getString(R.string.route_admin))
            R.id.network_page-> appendToIPAddress(resources.getString(R.string.route_network))
//            R.id.cloud_options-> showDialogCloudOptions()
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
    fun requestLocationPermission(): Int {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                //TODO What todo here?
            } else {
                // request permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_CODE_ACCEPTED
                )
            }
        } else {
            // already granted
            return PERMISSION_CODE_ACCEPTED
        }

        // not available
        return PERMISSION_CODE_NOT_AVAILABLE
    }

    private fun setIPAddressToLR125(route: String = "") {
        if (dialogLRNotFound?.isShowing == true) {
            dialogLRNotFound?.dismiss()
        }
        if (dialogNetworkScanInProgress?.isShowing == true) {
            dialogNetworkScanInProgress?.dismiss()
        }
        runOnUiThread {
            dialogNetworkScanInProgress = DialogNetworkScanInProgress(this)
            dialogNetworkScanInProgress?.show()
            dialogNetworkScanInProgress?.window?.setLayout(dialogSide, dialogSide)
        }

        val mdnsHandler = MdnsHandler(this)
        val bonjourServices = mdnsHandler.services
        var failedToFindLR125 = true
        Log.d("Test Point", "TP1")
        if (bonjourServices.isNotEmpty()) {
            Log.d("Test Point", "TP2")
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
        Log.d("Test Point", "TP3")
        if (failedToFindLR125) {
            Log.d("Test Point", "TP4")
            dialogNetworkScanInProgress?.cancel()

            runOnUiThread {
                showDialogLRNotFound()
            }
        }
    }

    private fun scanNetwork(route: String = "") {
        /*if(!wifiIsEnabled())
            showDialogWifiNotEnabled()
        else {*/
            CoroutineScope(Dispatchers.IO).launch {
                setIPAddressToLR125(route)
            }
       // }
    }

    private fun loadURL(url: String) {
        webView.post(Runnable {
            val protocol = resources.getString(R.string.protocol_http)
            webView.loadUrl("$protocol$url")
        })
    }

    fun isInternetAvailable(): Boolean {
        return try {
            val ipAddr: InetAddress = InetAddress.getByName("google.com")
            //You can replace it with your name
            !ipAddr.equals("")
        } catch (e: Exception) {
            false
        }
    }

    /***
     * TODO: Make sure R.string.url_cloud is correct
     * R.string.url_cloud might be set to test site. // currently correct.
     */
    private fun navigateToCloud() {
        webView.post(Runnable {
            if(isInternetAvailable()) {
                val urlCloud: String = resources.getString(R.string.url_cloud)
                webView.loadUrl(urlCloud)
            }
            else
            {
                showDialogNoInternet();
            }
        })
    }

    private fun refreshPage() {
        webView.post(Runnable {
            webView.reload()
        })
    }

    private fun showDialogNoInternet() {
        val dialogNoInternet = DialogNoInternet(this)
        dialogNoInternet.show()
        dialogNoInternet.window?.setLayout(dialogSide, dialogSide)

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
        dialogWifiNotEnabled = DialogWifiNotEnabled(this, webView)
        dialogWifiNotEnabled?.show()
        dialogWifiNotEnabled?.window?.setLayout(dialogSide, dialogSide)

        dialogWifiNotEnabled?.setOnCancelListener {
            if(callScanNetworkOnDialogClose) {
                scanNetwork()
            }
        }
    }

    private fun showDialogLRNotFound() {
       dialogLRNotFound = DialogLRNotFound(this, webView)
        dialogLRNotFound?.show()
        dialogLRNotFound?.window?.setLayout(dialogSide, dialogSide)
        dialogLRNotFound?.setOnCancelListener {
            if(callScanNetworkOnDialogClose) {
                scanNetwork()
            }
        }
    }

    private fun showDialogConnectToCloud() {
        dialogConnectToCloud = DialogConnectToCloud(this, webView)
        dialogConnectToCloud?.show()
        dialogConnectToCloud?.window?.setLayout(dialogSide, dialogSide)
        /*dialogConnectToCloud.setOnCancelListener {
            if(callScanNetworkOnDialogClose) {
                scanNetwork()
            }
        }*/
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

    private fun setNetworkChangeCallBack() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val link: LinkProperties? =
                connectivityManager.getLinkProperties(connectivityManager.activeNetwork)

            //Log.d("IP ADDRESS TP3?", link?.linkAddresses.toString())
            //Log.d("Network Name Tp1?", link?.interfaceName.toString())
            link ?: run {
                if (dialogWifiNotEnabled?.isShowing ?: false) {
                    dialogWifiNotEnabled?.dismiss()
                }

                showDialogWifiNotEnabled()
            }
        }
        connectivityManager.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                it.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.d("NETWORK", "Network has become available")
                        super.onAvailable(network)
                    }

                    override fun onLost(network: Network) {
                        Log.d("NETWORK", "Network has been lost")
                        val link: LinkProperties? =  connectivityManager.getLinkProperties(connectivityManager.activeNetwork)

                        //Log.d("IP ADDRESS? TP1", link?.linkAddresses.toString())
                        //Log.d("Network Name Tp2?", link?.interfaceName.toString())
                        link ?: run {
                            if (dialogNetworkScanInProgress?.isShowing ?: false) {
                                dialogNetworkScanInProgress?.dismiss()
                            }
                            if (dialogConnectToCloud?.isShowing ?: false) {
                                dialogConnectToCloud?.dismiss()
                            }
                            if (dialogLRNotFound?.isShowing ?: false) {
                                dialogLRNotFound?.dismiss()
                            }

                            if (dialogWifiNotEnabled?.isShowing ?: false) {
                                Log.d("Dialog", "Wifi Not enabled is showing")
                            } else {
                                showDialogWifiNotEnabled()
                            }
                        }
                        super.onLost(network)
                    }


                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        if (lastNetwork == network) {
                            Log.d("NETWORK", "Network is equal!!!")
                            return
                        } else {
                            Log.d("NETWORK", "NETWORK IS NOT EQUAL")
                        }

                        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            Log.d("TEST", "Found cellular type")
                            if (dialogNetworkScanInProgress?.isShowing ?: false) {
                                dialogNetworkScanInProgress?.dismiss()
                            }
                            if(dialogWifiNotEnabled?.isShowing ?: false) {
                                dialogWifiNotEnabled?.dismiss()
                            }
                            if (dialogLRNotFound?.isShowing ?: false) {
                                dialogLRNotFound?.dismiss()
                            }
                            lastNetwork = if (dialogConnectToCloud?.isShowing ?: false) {
                                network
                            } else {
                                showDialogConnectToCloud()
                                network
                            }
                            internetAvailable = isInternetAvailable()
                            Log.d("Avail 1", internetAvailable.toString())
                        } else {
                            if (dialogConnectToCloud?.isShowing ?: false) {
                                dialogConnectToCloud?.dismiss()
                            }
                            if(dialogWifiNotEnabled?.isShowing ?: false) {
                                dialogWifiNotEnabled?.dismiss()
                            }
                            if (dialogLRNotFound?.isShowing ?: false) {
                                dialogLRNotFound?.dismiss()
                            }
                            lastNetwork = if (dialogNetworkScanInProgress?.isShowing ?: false) {
                                network
                            } else {
                                scanNetwork()
                                network
                            }
                            internetAvailable = isInternetAvailable()
                            Log.d("Avail 2", internetAvailable.toString())
                        }
                        super.onCapabilitiesChanged(network, networkCapabilities)
                    }

                })


            }
        }
    }
    private fun ipToString(i: Int): String {
        return (i and 0xFF).toString() + "." +
                (i shr 8 and 0xFF) + "." +
                (i shr 16 and 0xFF) + "." +
                (i shr 24 and 0xFF)

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
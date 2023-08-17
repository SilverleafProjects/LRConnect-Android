//File used for the Winnebago LR125 Control app.

package com.silverleaf.winnebagocontrolandroid

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.*
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
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
import com.silverleaf.lrgizmo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import preferences.Preferences
import scannetwork.MdnsHandler
import webviewsettings.setWebView
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


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
        val lr125DataStorage = mutableMapOf<InetAddress, Pair<Long, String>?>()
        var udpListenerIsNotRunning: Boolean = true
        val udpListenerSocket = DatagramSocket(InetSocketAddress(4242))
        var LRConnectCalledFromSettingsMenu = false
        const val FILE_RESULT_CODE: Int = 69
        const val PERMISSION_CODE_ACCEPTED = 1
        const val PERMISSION_CODE_NOT_AVAILABLE = 0
        var internetAvailable: Boolean = false
        var failedToDiscoverLR: Boolean = true
        lateinit var preferences: Preferences
        val udpDetectCoroutine = CoroutineScope(Dispatchers.IO)

        /* Sentinel values used between classes */
        var callScanNetworkOnDialogClose: Boolean = false
        var closeLRDiscoveryDialog: Boolean = false
        var noDetectedLROnNetwork: Boolean = false

        /* Declarations for NSD Manager*/
        var usingMDNSLookup: Boolean = false
        lateinit var NSDManager: NsdManager
        var NSDListener: DiscoveryListener = DiscoveryListener()
        val lrDetectCoroutine = CoroutineScope(Dispatchers.IO)

        fun saveTokenToPreferences(token: String) {
            preferences.saveString("token", token)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferences = Preferences(this)
        if (preferences.retrieveString("token") != null) {
            val token = preferences.retrieveString("token")
            println("Found token: $token")
        }
        dialogSide = 9 * Math.min(
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels
        ) / 10

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
        when (item.itemId) {
            R.id.main_page -> returnToHomeScreen("")
            R.id.refresh -> refreshPage()
            R.id.scan_network -> scanNetwork()
            R.id.enter_ip_address -> showDialogEnterIPAddress()
            R.id.clear_cache -> clearBrowserCache()
            R.id.admin_page -> appendToIPAddress(resources.getString(R.string.route_admin))
            R.id.network_page -> appendToIPAddress(resources.getString(R.string.route_network))
            R.id.cloud_page -> navigateToCloud()

            else -> println("default")
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FILE_RESULT_CODE -> {
                if (RESULT_OK == resultCode) {
                    valueCallBack?.onReceiveValue(
                        WebChromeClient.FileChooserParams.parseResult(
                            resultCode,
                            data
                        )
                    )
                }
            }
        }
    }

    fun requestLocationPermission(): Int {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                //TODO What todo here?
            } else {
                // request permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
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

    private fun startUDPListenerThread() {
        CoroutineScope(Dispatchers.IO).launch {
            while (lr125DataStorage.isEmpty()) udpMessageListener()
        }
        udpListenerIsNotRunning = false
    }


    fun scanhNetwork(route: String = "") {
        if ((udpListenerIsNotRunning) || (lr125DataStorage.isEmpty())) startUDPListenerThread()
        while (lr125DataStorage.isEmpty()) {
            if (lr125DataStorage.isNotEmpty()) break
        }
        if(LRConnectCalledFromSettingsMenu) {
            findLR125WithoutConnection(route)
            LRConnectCalledFromSettingsMenu = false
        }
        else setIPAddressToLR125(route)

    }

    fun scanNetwork(route: String=""){
        runOnUiThread {
            dialogNetworkScanInProgress = DialogNetworkScanInProgress(this)
            dialogNetworkScanInProgress?.show()
            dialogNetworkScanInProgress?.window?.setLayout(dialogSide, dialogSide)
        }

        udpDetectCoroutine.launch {
            udpMessageListener()
            println("lr125: ${lr125DataStorage.isEmpty()}")
            while (lr125DataStorage.isEmpty()){
                Log.d("Test Point", "While Loop")
                if (lr125DataStorage.isNotEmpty()) break
            }
        }


        if(noDetectedLROnNetwork){
            usingMDNSLookup = true
            lrDetectCoroutine.launch {
                NSDManager.discoverServices(
                    "_http._tcp",
                    NsdManager.PROTOCOL_DNS_SD,
                    NSDListener
                )
            }
        }

        Handler().postDelayed({
            if(lr125DataStorage.isEmpty() && (NSDListener.serviceList.size == 0)){
                showDialogLRNotFound()
            }
            else {
                setIPAddressToLR125(route)
            }
        },4000)
    }

    private fun setIPAddressToLR125(route: String = "") {

        if (dialogLRNotFound?.isShowing == true) {
            dialogLRNotFound?.dismiss()
        }
        if (dialogNetworkScanInProgress?.isShowing == true) {
            dialogNetworkScanInProgress?.dismiss()
        }

        val currentTime = System.currentTimeMillis() / 1000
        var failedToFindLR125 = true

        timeoutIfLRIsNotDetected()

        if (lr125DataStorage.isNotEmpty() && !usingMDNSLookup)
        {
            dialogNetworkScanInProgress?.cancel()
            callScanNetworkOnDialogClose = false
            for (entry in lr125DataStorage) {
                if (compareTimeStamps(entry?.value!!.first, currentTime)) {
                    if (ipAddressIsValid(entry.key.toString())) {
                        if (isValidSilverLeafDevice(entry.value!!.second)) {
                            ipAddress = entry.key.toString()
                            loadURL("$ipAddress/$route")
                            failedToFindLR125 = false
                            failedToDiscoverLR = false

                            if (dialogNetworkScanInProgress?.isShowing == true) {
                                dialogNetworkScanInProgress?.dismiss()
                                callScanNetworkOnDialogClose = false
                            }
                        }
                    }
                }
            }
        }

        if(NSDListener.serviceList.size > 0 && usingMDNSLookup)
        {
            var firstDetectedLR = NSDListener.serviceList[0]
            try {
                var nsdResolveListener: NsdManager.ResolveListener = object : NsdManager.ResolveListener {
                    override fun onResolveFailed(detectedLR: NsdServiceInfo?, p1: Int) {
                        failedToFindLR125 = true
                    }
                    override fun onServiceResolved(detectedLR: NsdServiceInfo?) {
                        if(detectedLR != null) loadURL(detectedLR.host.toString())
                    }
                }
                NSDManager.resolveService(firstDetectedLR, nsdResolveListener)
                NSDManager.stopServiceDiscovery(NSDListener)

                if (dialogNetworkScanInProgress?.isShowing == true) {
                    dialogNetworkScanInProgress?.dismiss()
                    callScanNetworkOnDialogClose = false
                }

                failedToFindLR125 = false
                failedToDiscoverLR = false
            }catch(e: Exception){
                e.printStackTrace()
            }
        }
        if (failedToFindLR125) {
            callScanNetworkOnDialogClose = false
            dialogNetworkScanInProgress?.cancel()
            showDialogLRNotFound()
        }
    }



/*
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

        val currentTime = System.currentTimeMillis() / 1000
        var failedToFindLR125 = true

        timeoutIfLRIsNotDetected()

        Log.d("Test Point", "TP1")
        if (lr125DataStorage.isNotEmpty()) {
            Log.d("Test Point", "TP2")
            dialogNetworkScanInProgress?.cancel()
            callScanNetworkOnDialogClose = false

            for (entry in lr125DataStorage) {
                if (compareTimeStamps(entry?.value!!.first, currentTime)) {
                    if (ipAddressIsValid(entry.key.toString())) {
                        if (isValidSilverLeafDevice(entry.value!!.second)) {
                            ipAddress = entry.key.toString()
                            loadURL("$ipAddress/$route")
                            failedToFindLR125 = false
                            break
                        }
                    }
                }
            }
        }
        if (failedToFindLR125 && (lr125DataStorage.isEmpty())) {
            val mdnsHandler = MdnsHandler(this)

            val bonjourServices = mdnsHandler.services
            var failedToFindLR125 = true

            if (bonjourServices.isNotEmpty()) {
                dialogNetworkScanInProgress?.cancel()
                callScanNetworkOnDialogClose = false

                for (i in 0 until bonjourServices.size) {
                    println(bonjourServices[i].inet4Address.toString())
                    if (ipAddressIsValid(bonjourServices[i].inet4Address.toString())) {
                        ipAddress = bonjourServices[i].inet4Address.toString()
                        loadURL("$ipAddress/$route")
                        failedToFindLR125 = false
                        break
                    }
                }
            }
        }
        if (failedToFindLR125) {
            callScanNetworkOnDialogClose = false
            dialogNetworkScanInProgress?.cancel()

            runOnUiThread {
                showDialogLRNotFound()
            }
        }
        try {
            dialogNetworkScanInProgress!!.cancel()
            if (dialogNetworkScanInProgress?.isShowing == true) dialogNetworkScanInProgress?.cancel()
            callScanNetworkOnDialogClose = false

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
*/
    private fun findLR125WithoutConnection(route: String = ""): Unit{

        val currentTime = System.currentTimeMillis() / 1000

        if (lr125DataStorage.isNotEmpty()) {
            Log.d("Test Point", "TP2")
            dialogNetworkScanInProgress?.cancel()
            callScanNetworkOnDialogClose = false

            for (entry in lr125DataStorage) {
                if (compareTimeStamps(entry?.value!!.first, currentTime)) {
                    if (ipAddressIsValid(entry.key.toString())) {
                        if (isValidSilverLeafDevice(entry.value!!.second)) {
                            ipAddress = entry.key.toString()
                            break
                        }
                    }
                }
            }
        }
    }

    private fun timeoutIfLRIsNotDetected(): Unit {
        val backgroundExecutor: ScheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor()

        backgroundExecutor.schedule({
            if ((dialogNetworkScanInProgress?.isShowing == true) && (ipAddress == null)) {
                dialogNetworkScanInProgress!!.cancel()
                callScanNetworkOnDialogClose = false
                dialogLRNotFound?.show()
            } else {
                dialogNetworkScanInProgress!!.cancel()
                callScanNetworkOnDialogClose = false
            }
        }, 10, TimeUnit.SECONDS)
    }

    private fun convertByteArray(byteArray: ByteArray): String {
        var returnString: String = ""
        for (byte in byteArray) returnString += byte.toInt().toChar()
        return returnString
    }

    private fun udpMessageListener(): Unit {
        try {
            val buffer = ByteArray(4096)
            val timestamp = System.currentTimeMillis() / 1000
            val udpPacket = DatagramPacket(buffer, buffer.size)

            try {
                udpListenerSocket.receive(udpPacket)
            } catch (e: Exception) {
            }
            var incomingAddress = udpPacket.address
            var incomingMessage = convertByteArray(udpPacket.data)

            var timestampedPacket = Pair(first = timestamp, second = incomingMessage)

            if (incomingAddress != null) {
                lr125DataStorage.put(incomingAddress, timestampedPacket)

            } else {
                udpMessageListener()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*******************************************************/

    private fun isValidSilverLeafDevice(messageString: String): Boolean {
        return (messageString.contains("LR125")) || (messageString.contains("RVHALO"))
    }

    private fun compareTimeStamps(savedDate: Long, currentDate: Long): Boolean {
        return (currentDate - savedDate) < 60
    }

    private fun loadURL(url: String) {
        webView.post(Runnable {
            val protocol = resources.getString(R.string.protocol_http)
            webView.loadUrl("$protocol$url")
        })
    }
//I'm keeping this section if we ever decide to use a more modern way of determining internet connection. (requires minSDK of 23)
/*
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if(connectivityManager != null) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if(capabilities != null){
                if(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return true
                else if(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return true
                else if(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return true
            }
        }
        return false
    }
*/

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
                if (isInternetAvailable()) { //isInternetAvailable(applicationContext)
                    val urlCloud: String = resources.getString(R.string.url_cloud)
                    webView.loadUrl(urlCloud)
                } else {
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

    private fun showDialogNoCloudService() {
        val dialogNoCloudService = DialogNoCloudService(this)
        dialogNoCloudService.show()
        dialogNoCloudService.window?.setLayout(dialogSide, dialogSide)
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
        val widthDialog: Int = Resources.getSystem().displayMetrics.widthPixels * 9 / 10
        val heightDialog: Int = Resources.getSystem().displayMetrics.heightPixels * 7 / 10

        dialogWifiNotEnabled = DialogWifiNotEnabled(this, webView)
        dialogWifiNotEnabled?.show()
        dialogWifiNotEnabled?.window?.setLayout(widthDialog, heightDialog)

        dialogWifiNotEnabled?.setOnCancelListener {
            if(callScanNetworkOnDialogClose) {
                scanNetwork()
            }
        }
    }

    private fun showDialogLRNotFound() {
        callScanNetworkOnDialogClose = true
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

    fun appendToIPAddress(route: String) {
        if(ipAddress != null) {
            if (wifiIsEnabled()) {
                if (ipAddressIsValid(ipAddress)) {
                    loadURL("$ipAddress/$route")
                    return
                }
                scanNetwork(route)
            } else {
                showDialogWifiNotEnabled()
            }
        }else return
    }

    private fun returnToHomeScreen(route: String) {
        if((ipAddress != null) && (wifiIsEnabled()))
        {
            if(ipAddressIsValid(ipAddress))
            {
                loadURL("$ipAddress/$route")
                return
            }
        }else return
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
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val link: LinkProperties? =
                connectivityManager.getLinkProperties(connectivityManager.activeNetwork)

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
                            if (dialogNetworkScanInProgress?.isShowing == true) {
                                dialogNetworkScanInProgress?.dismiss()
                            }
                            if (dialogConnectToCloud?.isShowing == true) {
                                dialogConnectToCloud?.dismiss()
                            }
                            if (dialogLRNotFound?.isShowing == true) {
                                dialogLRNotFound?.dismiss()
                            }

                            if (dialogWifiNotEnabled?.isShowing == true) {
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
                            internetAvailable = isInternetAvailable() //isInternetAvailable(applicationContext)
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
                            internetAvailable = isInternetAvailable() //isInternetAvailable(applicationContext)
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

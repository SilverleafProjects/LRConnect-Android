//File used for the standard LR125 Control app.
package com.silverleaf.winnebagocontrolandroid

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.ResolveListener
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import com.silverleaf.lrgizmo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.json.JSONObject
import preferences.Preferences
import webviewsettings.setWebView
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


/* CURRENT VERSION:  8: 1.06 */

class ScreenStatusViewModel : ViewModel() {

        val currentStatus: MutableLiveData<Boolean> by lazy {
            MutableLiveData<Boolean>()
        }
}

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    lateinit var progressBar: ProgressBar

    var dialogSide: Int = 0

    private val viewModel: ScreenStatusViewModel by viewModels()
    private var dialogNetworkScanInProgress: DialogNetworkScanInProgress? = null
    private var dialogConnectToCloud: DialogConnectToCloud? = null
    private var dialogWifiNotEnabled: DialogWifiNotEnabled? = null
    private var dialogUserInformation: DialogUserInformation? = null
    private var dialogLRNotFound: DialogLRNotFound? = null
    private var lastNetwork: Network? = null
    private val lifecycleListener: LifecycleListener by lazy {
        LifecycleListener(webView)
    }

    companion object {

        var ipAddress: String = ""
        var valueCallBack: ValueCallback<Array<Uri>>? = null
        val lr125DataStorage = mutableMapOf<InetAddress, Pair<Long, String>?>()
        var cloudServiceStatus: Boolean = false
        var screenAlwaysOn: Boolean = false
        var udpListenerIsNotRunning: Boolean = true
        val udpListenerSocket = DatagramSocket(InetSocketAddress(4242))
        const val FILE_RESULT_CODE: Int = 69
        const val PERMISSION_CODE_ACCEPTED = 1
        const val PERMISSION_CODE_NOT_AVAILABLE = 0
        var internetAvailable: Boolean = false
        lateinit var preferences: Preferences
        val udpDetectCoroutine = CoroutineScope(Dispatchers.IO)
        var failedToDiscoverLR: Boolean = true
        var usersVariantOfRozie: String = String()

        var email_id: String = ""
        var sms_id: String = ""
        var push_id: String = ""

        /* Sentinel values used between classes */
        var callScanNetworkOnDialogClose: Boolean = false
        var noDetectedLROnNetwork: Boolean = false
        var goToCloud: Boolean = false
        var goToCloudLogin: Boolean = false
        var isConnectedToLR: Boolean = false
        var isConnectedToCloud: Boolean = false
        var isHTTPAutoLoginEnabled: String = ""
        var getUserInformationOnClose: Boolean = false

        /* Declarations for NSD Manager*/
        var usingMDNSLookup: Boolean = false
        lateinit var NSDManager: NsdManager
        var NSDListener: DiscoveryListener = DiscoveryListener()
        val lrDetectCoroutine = CoroutineScope(Dispatchers.IO)

        /* Declarations for HTTP requests */
        val client = OkHttpClient()
        var httpAccessToken: String = String()
        var userEnteredCredentials: Boolean = false
        var tokenValidStartTime:Long = 0
        var winegardAccessToken: String = String()
        var winegardIdToken: String = String()
        var winegardRefreshToken: String = String()
        var registerToRozieURL: String = String()
        private var haveNotificationsBeenRecieved: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NSDManager = getSystemService(Context.NSD_SERVICE) as NsdManager

        preferences = Preferences(this)
        if (preferences.retrieveString("token") != null) {
            val token = preferences.retrieveString("token")
        }

        try {

            if (preferences.retrieveBoolean("screenAlwaysOnStatus") != null) {
                val screenstat = preferences.retrieveBoolean("screenAlwaysOnStatus")
                screenAlwaysOn = screenstat
                println("Screen status: $screenstat")
            } else screenAlwaysOn = false
            if (preferences.retrieveBoolean("cloudServiceStatus") != null) {
                val cloudstat = preferences.retrieveBoolean("cloudServiceStatus")
                cloudServiceStatus = cloudstat
                println("Cloud status: $cloudstat")
            } else cloudServiceStatus = false


        } catch (e: Exception) {
            e.printStackTrace()
        }

        dialogSide = 9 * Math.min(
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels
        ) / 10

        usersVariantOfRozie = when (preferences.retrieveString("RozieVersion")){
            "Rozie Core Services" -> "roziecoreservices.com"//println("Variant: Rozie Core") //Winnebago Only
            "Rozie 2" -> "roziecoreservices.com/rozie2"//println("Variant: Rozie 2") //Scott's APE
            "MyRozie" -> "myrozie.com/"
            "None" -> ""
            else -> "myrozie.com/"
        }

        bindUI()
        if(!preferences.retrieveBoolean("HasUserSelectedCoachModel")) {
            showDialogModelAndYear()
        }
        else{
            setNetworkChangeCallBack()
        }
        setupLifecycleListener()

        if((preferences.retrieveString("AccessToken") != null) && (hasAccessTokenTimedOut(System.currentTimeMillis(), tokenValidStartTime))) {
            val actoken = preferences.retrieveString("AccessToken")
            val idtoken = preferences.retrieveString("IDToken")
            val rftoken = preferences.retrieveString("RefreshToken")

            if(actoken != null) {
                winegardAccessToken = actoken
            }

            if(idtoken != null) {
                winegardIdToken = idtoken
            }

            if(rftoken != null) {
                winegardRefreshToken = rftoken
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        try {
            if (screenAlwaysOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

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
            R.id.scan_network -> scanNetwork("")
            R.id.enter_ip_address -> showDialogEnterIPAddress()
            R.id.clear_cache -> clearBrowserCache()
            R.id.admin_page -> appendToIPAddress(resources.getString(R.string.route_admin))
            R.id.network_page -> appendToIPAddress(resources.getString(R.string.route_network))
            R.id.cloud_page -> navigateToCloud()
            R.id.systemcontrol_settingpage -> navigateToSettingsPage()
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

    fun scanNetwork(route: String=""){
        isConnectedToLR = false
        isConnectedToCloud = false
        usingMDNSLookup = false

        if((preferences.retrieveString("httpLoginSetting") == "Auto Cloud Login On") && (preferences.retrieveBoolean("cloudServiceStatus"))){
            if((preferences.retrieveString("AccessToken") != null) && (hasAccessTokenTimedOut(System.currentTimeMillis(), tokenValidStartTime))) {
                val actoken = preferences.retrieveString("AccessToken")
                val idtoken = preferences.retrieveString("IDToken")
                val rftoken = preferences.retrieveString("RefreshToken")
                webView.post(kotlinx.coroutines.Runnable {
                    webView.loadUrl("https://www.roziecoreservices.com/rozie2?accessToken=${actoken}&idToken=${idtoken}&refreshToken=${rftoken}")
                })

                val autoConnectionToast = Toast.makeText(this.applicationContext, "Automatically connected to Rozie Core Services.", Toast.LENGTH_SHORT)
                autoConnectionToast.show()

                isConnectedToCloud = true
            }else{
                navigateToCloud()
            }
        }
        else
        {
            runOnUiThread {
                dialogNetworkScanInProgress = DialogNetworkScanInProgress(this)
                dialogNetworkScanInProgress?.show()
                dialogNetworkScanInProgress?.window?.setLayout(dialogSide, dialogSide)
            }

            timeoutIfLRIsNotDetected()
            udpDetectCoroutine.launch {
                udpMessageListener()
                while (lr125DataStorage.isEmpty()) {
                    if (lr125DataStorage.isNotEmpty()) break
                }
            }

            if (noDetectedLROnNetwork) {
                isConnectedToLR = false
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
                if (lr125DataStorage.isEmpty() && (NSDListener.serviceList.size == 0)) {
                    showDialogLRNotFound()
                } else {
                    isConnectedToLR = true
                    setIPAddressToLR125(route)
                }
            }, 3000)
        }
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
                var nsdResolveListener: ResolveListener = object : NsdManager.ResolveListener {
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
                }

                failedToFindLR125 = false
                failedToDiscoverLR = false
            }catch(e: Exception){
                e.printStackTrace()
            }
        }
        if (failedToFindLR125) {
            dialogNetworkScanInProgress?.cancel()
            showDialogLRNotFound()
        }
    }

    private fun udpMessageListener(): Unit {

            udpListenerSocket.soTimeout = 5000
        try {
                val buffer = ByteArray(4096)
                val timestamp = System.currentTimeMillis() / 1000
                val udpPacket = DatagramPacket(buffer, buffer.size)

                udpListenerSocket.receive(udpPacket)

                var incomingAddress = udpPacket.address
                var incomingMessage = convertByteArray(udpPacket.data)

                var timestampedPacket = Pair(first = timestamp, second = incomingMessage)

                if (incomingAddress != null) {
                    lr125DataStorage.put(incomingAddress, timestampedPacket)
                    noDetectedLROnNetwork = false
                } else {
                    udpMessageListener()
                }
        } catch (e: SocketTimeoutException) {
            e.printStackTrace()
            udpDetectCoroutine.cancel()
        }
    }
    private fun timeoutIfLRIsNotDetected(): Unit {
        val backgroundExecutor: ScheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor()

        backgroundExecutor.schedule({
            if ((dialogNetworkScanInProgress?.isShowing == true) && (ipAddress == "")) {
                dialogNetworkScanInProgress!!.cancel()
                dialogLRNotFound?.show()
            } else {
                dialogNetworkScanInProgress!!.cancel()
            }
        }, 5, TimeUnit.SECONDS)
    }

    /*******************************************************/
    private fun isValidSilverLeafDevice(messageString: String): Boolean {
        return (messageString.contains("LR125")) || (messageString.contains("RVHALO"))
    }

    private fun compareTimeStamps(savedDate: Long, currentDate: Long): Boolean {
        return (currentDate - savedDate) < 60
    }

    private fun convertByteArray(byteArray: ByteArray): String {
        var returnString: String = ""
        for (byte in byteArray) returnString += byte.toInt().toChar()
        return returnString
    }

    public fun hasAccessTokenTimedOut(currentTime: Long, tokenValidationTime: Long): Boolean {
        val expiration_time = preferences.retrieveInt("AccessTimeout")
        return currentTime > (tokenValidationTime + (expiration_time * 1000))
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

    private fun currentRozieAddress(): String{
        when(preferences.retrieveString("RozieVersion").toString()){
            "MyRozie" -> return "myrozie.com/"
            "Rozie 2" -> return "identity.winegard-staging.io/login/"
            "Rozie Core Services" -> return "roziecoreservices.com"
            "None" -> return "None"
        }
        return "Error"
    }

    fun isInternetAvailable(): Boolean {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    public fun accessKeyHasTimedOut(): Boolean{
        return System.currentTimeMillis() > (preferences.retrieveLong(
            "TokenStartTime"
        ) + (preferences.retrieveInt("AccessTimeout") * 1000))
    }

    /***
     * TODO: Make sure R.string.url_cloud is correct
     * R.string.url_cloud might be set to test site. // currently correct.
     */

    public fun navigateToCloud()
    {
        if(cloudServiceStatus || (preferences.retrieveString("RozieVersion") != "None")){

            if(preferences.retrieveString("RozieVersion") != "Rozie 2")
            {
                loadURL(currentRozieAddress())
                isConnectedToCloud = true
            }else if(preferences.retrieveString("RozieVersion") == "Rozie 2"){

                if((preferences.retrieveString("AccessToken") == null) || (accessKeyHasTimedOut())) runBlocking {showDialogUserInformation()} //if access has timed out or we have no token, prompt user information
                else{
                    val actoken = preferences.retrieveString("AccessToken")
                    val idtoken = preferences.retrieveString("IDToken")
                    val rftoken = preferences.retrieveString("RefreshToken")
                    webView.post(kotlinx.coroutines.Runnable {
                        webView.loadUrl("https://www.roziecoreservices.com/rozie2?accessToken=${actoken}&idToken=${idtoken}&refreshToken=${rftoken}")
                    })
                    isConnectedToCloud = true
                }
            }

        }else{
            showDialogNoCloudService()
            isConnectedToCloud = false
        }
    }

    public fun registerToRozieCoreServices()
    {
        if(cloudServiceStatus) {
            if (preferences.retrieveString("AccessToken") != null) {
                registerToRozieURL =
                    "register/rozie2register.php?accessToken=${
                        preferences.retrieveString(
                            "AccessToken"
                        )
                    }&idToken=${
                        preferences.retrieveString("IDToken")
                    }&refreshToken=${preferences.retrieveString("RefreshToken")}"

                webView.post(kotlinx.coroutines.Runnable{
                    appendToIPAddress(registerToRozieURL)
                })
            } else {
                showDialogUserInformation()
            }
        }else{
            showDialogNoCloudService()
        }
    }

    private fun refreshPage() {
        webView.post(Runnable {
            webView.reload()
        })
    }

    private fun showDialogIncorrectCredentials() {
        val dialogIncorrectCredentials = DialogIncorrectCredentials(this)
        dialogIncorrectCredentials.show()
        dialogIncorrectCredentials.window?.setLayout(dialogSide, dialogSide)
    }

    private fun showDialogModelAndYear() {
        //Rozie Core == 1 //Winnebago Only
        //Rozie 2 == 2    //Modern API
        //MyRozie == 3    //Old Rozie Login
        //None == 4       //None
        val dialogInitialModelAndYear = DialogModelAndYear(this)
        dialogInitialModelAndYear.show()
        dialogInitialModelAndYear.window?.setLayout(dialogSide, dialogSide)
        dialogInitialModelAndYear.setOnCancelListener {
            if(goToCloudLogin){
                cloudServiceStatus = true
                navigateToCloud()
            }
        }
    }

    public fun showDialogUserInformation() {
        val dialogUserInformation = DialogUserInformation(this, webView)
        dialogUserInformation.show()
        dialogUserInformation.window?.setLayout(dialogSide, dialogSide)
    }

    public fun wineGardLogout(){
        MainActivity.preferences.saveInt("AccessTimeout", 0)
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

    private fun navigateToSettingsPage() {

        val widthDialog: Int = Resources.getSystem().displayMetrics.widthPixels * 9 / 10
        val heightDialog: Int = Resources.getSystem().displayMetrics.heightPixels * 9 / 10

        val dialogSettingsMenu = DialogSystemControlSettings(this, webView)

        if(!dialogSettingsMenu.isShowing){
        dialogSettingsMenu.show()
            dialogSettingsMenu.window?.setLayout(widthDialog, heightDialog)
        }
        dialogSettingsMenu.setOnCancelListener {

            if(getUserInformationOnClose)
            {
                navigateToCloud()
                getUserInformationOnClose = false
            }
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

        val widthDialog: Int = Resources.getSystem().displayMetrics.widthPixels * 9 / 10
        val heightDialog: Int = Resources.getSystem().displayMetrics.heightPixels * 5 / 10

        callScanNetworkOnDialogClose = true
       dialogLRNotFound = DialogLRNotFound(this, webView)
        dialogLRNotFound?.show()
        dialogLRNotFound?.window?.setLayout(widthDialog, heightDialog)

        dialogLRNotFound?.setOnCancelListener {
            if(callScanNetworkOnDialogClose) {
                goToCloud = false
                scanNetwork()
            }
            if(goToCloud){
                println("Cloud Service: ${preferences.retrieveBoolean("screenAlwaysOnStatus")}")
                if(!cloudServiceStatus){
                    cloudServiceStatus = true
                    preferences.saveBoolean("screenAlwaysOnStatus", true)
                    println("Cloud Service is set to: ${preferences.retrieveBoolean("screenAlwaysOnStatus")}")
                }
                if (dialogLRNotFound?.isShowing == true) {
                    dialogLRNotFound?.dismiss()
                }
                if (dialogNetworkScanInProgress?.isShowing == true) {
                    dialogNetworkScanInProgress?.dismiss()
                }
            }
        }
    }

    private fun showDialogConnectToCloud() {
        if(cloudServiceStatus) {
            dialogConnectToCloud = DialogConnectToCloud(this, webView)
            dialogConnectToCloud?.show()
            dialogConnectToCloud?.window?.setLayout(dialogSide, dialogSide)
        }else{
            showDialogNoCloudService()
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

    public fun setNetworkChangeCallBack() {
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

                            webView.post(Runnable {
                                internetAvailable = isInternetAvailable() //isInternetAvailable(applicationContext)
                                Log.d("Avail 1", internetAvailable.toString())

                            })

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

                            webView.post(Runnable {
                                internetAvailable = isInternetAvailable() //isInternetAvailable(applicationContext)
                                Log.d("Avail 2", internetAvailable.toString())
                            })
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


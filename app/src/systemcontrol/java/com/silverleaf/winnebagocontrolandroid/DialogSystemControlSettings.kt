package com.silverleaf.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import com.silverleaf.lrgizmo.R
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.core.view.isVisible
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.silverleaf.lrgizmo.R.*
import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.email_id
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.screenAlwaysOn
import kotlinx.coroutines.*
import okhttp3.internal.http.HTTP_GONE
import org.json.JSONArray
import org.json.JSONObject
import java.util.MissingFormatArgumentException

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
//import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import com.google.firebase.messaging.remoteMessage

class DialogSystemControlSettings(activity: Activity, webView: WebView): Dialog(activity) {

    private lateinit var buttonAdvancedSettings:   Button
    private lateinit var buttonConnectionSettings: Button
    private lateinit var buttonOemSettings:        Button

    private lateinit var CloudBtn: Button

//    private lateinit var smsCheckBox:   CheckBox
    private lateinit var emailCheckBox: CheckBox
    private lateinit var pushCheckbox: CheckBox
    private lateinit var rozieVersionSpinner: Spinner
    private lateinit var displaySpinner: Spinner

    private lateinit var yearSpinner: Spinner
    private lateinit var coachModelSpinner: Spinner

    private lateinit var returnToAppButton:    Button

    private var haveNotificationsBeenRecieved: Boolean = false

    private var webView: WebView
    private var activity: MainActivity

    init {
        setCancelable(false)
        webView.also { this.webView = it }
        activity.also { this.activity = it as MainActivity }
    }

    fun saveScreenStatus(token: Boolean) {
            MainActivity.preferences.saveBoolean("screenAlwaysOnStatus", token)
    }

    fun saveCloudStatus(token: Boolean) {
        MainActivity.preferences.saveBoolean("cloudServiceStatus", token)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.dialog_advanced_settings)

        val widthDialog: Int = Resources.getSystem().displayMetrics.widthPixels * 9 / 10
        val heightDialog: Int = Resources.getSystem().displayMetrics.heightPixels * 9 / 10

//        smsCheckBox = findViewById<CheckBox>(R.id.smsAlertCheckbox)
        emailCheckBox = findViewById<CheckBox>(R.id.emailAlertCheckbox)
        pushCheckbox = findViewById<CheckBox>(R.id.pushAlertCheckbox)
        rozieVersionSpinner = findViewById<Spinner>(id.RozieVersionSpinner)

        yearSpinner = findViewById<Spinner>(id.VersionSpinner)
        coachModelSpinner = findViewById<Spinner>(id.CoachModelSpinner)


        val GeneralTab = findViewById<LinearLayout>(R.id.GeneralTab);
        val AppTab = findViewById<LinearLayout>(R.id.AppTab);
        val HardwareTab = findViewById<LinearLayout>(R.id.HardwareTab);
        val CloudTab = findViewById<LinearLayout>(R.id.CloudTab);


        CloudBtn = findViewById<Button>(R.id.CloudBtn);

        val currentRozieVersion: String = if(MainActivity.preferences.retrieveString("RozieVersion") == null) "None"
        else MainActivity.preferences.retrieveString("RozieVersion")!!

        var regBtn = findViewById<Button>(R.id.RegisterBtn)

        if(currentRozieVersion != "Rozie 2") {
            CloudBtn.visibility = GONE
            regBtn.visibility = GONE
        }
        else if(activity.accessKeyHasTimedOut()){
            regBtn.visibility = GONE
        }
        else {
            regBtn.visibility = VISIBLE
        }

        GeneralTab.visibility  = LinearLayout.VISIBLE
        AppTab.visibility      = LinearLayout.GONE
        HardwareTab.visibility = LinearLayout.GONE
        CloudTab.visibility    = LinearLayout.GONE

        findViewById<Button>(R.id.GeneralBtn).setOnClickListener {
            GeneralTab.visibility  = LinearLayout.VISIBLE
            AppTab.visibility      = LinearLayout.GONE
            HardwareTab.visibility = LinearLayout.GONE
            CloudTab.visibility    = LinearLayout.GONE
        };

        findViewById<Button>(R.id.AppBtn).setOnClickListener {
            GeneralTab.visibility  = LinearLayout.GONE
            AppTab.visibility      = LinearLayout.VISIBLE
            HardwareTab.visibility = LinearLayout.GONE
            CloudTab.visibility    = LinearLayout.GONE
        };

        findViewById<Button>(R.id.HardwareBtn).setOnClickListener {
            GeneralTab.visibility  = LinearLayout.GONE
            AppTab.visibility      = LinearLayout.GONE
            HardwareTab.visibility = LinearLayout.VISIBLE
            CloudTab.visibility    = LinearLayout.GONE
        };

        CloudBtn.setOnClickListener {
            GeneralTab.visibility  = LinearLayout.GONE
            AppTab.visibility      = LinearLayout.GONE
            HardwareTab.visibility = LinearLayout.GONE
            CloudTab.visibility    = LinearLayout.VISIBLE
        };

        findViewById<Button>(R.id.LoginBtn).setOnClickListener {
            activity.showDialogUserInformation()
        };

        findViewById<Button>(R.id.Logout_Btn).setOnClickListener {
            activity.wineGardLogout()
        };

        regBtn.setOnClickListener {
            activity.registerToRozieCoreServices()
        };

        findViewById<Button>(R.id.ClearTokens_Btn).setOnClickListener{
            MainActivity.preferences.saveString("FBToken", "")
            activity.getFBToken()
        }


        var loggedOutTab = findViewById<LinearLayout>(R.id.CloudTabLoggedOut);
        var loggedInTab = findViewById<LinearLayout>(R.id.CloudTabLoggedIn);

        if(activity.accessKeyHasTimedOut()){
            loggedInTab.visibility  = GONE
            loggedOutTab.visibility = VISIBLE
        }
        else{
            loggedInTab.visibility  = VISIBLE
            loggedOutTab.visibility = GONE

            getCurrentlyActiveNotifications()
        }
        if(MainActivity.ipAddress != "") {
            findViewById<TextView>(R.id.HardwareIP).setText(MainActivity.ipAddress)
        }

//        val getTokenBtn = findViewById<Button>(R.id.TokenBtn)
//        val tokenText = findViewById<TextView>(R.id.TokenTxt)

//        getTokenBtn.setOnClickListener{
////            tokenText.text = "test"
//            logRegToken(tokenText)
//        }

        bindUI()
    }

    fun logRegToken(tokenText: TextView) {
        // [START log_reg_token]
        Firebase.messaging.getToken().addOnCompleteListener { task ->
            if (!task.isSuccessful) {

                tokenText.text = task.exception?.stackTraceToString() ?: "Error"
                return@addOnCompleteListener
            }

            tokenText.text = task.result
        }
        // [END log_reg_token]
    }

    private fun getIPAddress(): String {
        val ipAddress = MainActivity.ipAddress

        val protocol = context.resources.getString(string.protocol_http)
        return "$protocol$ipAddress"
    }

    private fun extractNotificationType(array: JSONArray?){
        if (array != null) {
            if(array.length() == 0){
                    MainActivity.preferences.saveBoolean("areEmailNotificationsActive", false)
                    MainActivity.preferences.saveBoolean("areSMSNotificationsActive", false)
                    MainActivity.preferences.saveBoolean("arePushNotificationsActive", false)
                haveNotificationsBeenRecieved = true
                return
            }

            GlobalScope.launch (Dispatchers.IO) {
                withContext (Dispatchers.Main) {
                    try {
                        for (i in 0 until array.length()) {
                            var j = JSONObject(array[i].toString())
                            if (j.get("notification_type") == "email") {
                                MainActivity.preferences.saveBoolean("areEmailNotificationsActive", true)
                                emailCheckBox.isChecked = MainActivity.preferences.retrieveBoolean("areEmailNotificationsActive")
                                //MainActivity.email_id = j.get("id").toString()
                            } else MainActivity.preferences.saveBoolean("areEmailNotificationsActive", false)

//                        if(j.get("notification_type") == "sms"){
//                            MainActivity.preferences.saveBoolean("areSMSNotificationsActive", true)
//                            smsCheckBox.isChecked = MainActivity.preferences.retrieveBoolean("areSMSNotificationsActive")
////                            MainActivity.sms_id = j.get("id").toString()
//                        }else MainActivity.preferences.saveBoolean("areSMSNotificationsActive", false)

                            if (j.get("notification_type") == "push") {
                                MainActivity.preferences.saveBoolean("arePushNotificationsActive", true)
                                pushCheckbox.isChecked = MainActivity.preferences.retrieveBoolean("arePushNotificationsActive")
//                            MainActivity.push_id = j.get("id").toString()
                            } else MainActivity.preferences.saveBoolean("arePushNotificationsActive", false)
                        }
                        haveNotificationsBeenRecieved = true
                    }
                    catch (e: Exception){
                        println(e);
                    }
                }
            }
        }
    }

    private fun getCurrentlyActiveNotifications(){
        val getNotificationRequest = Request.Builder()
            .url("https://0ehkztwewg.execute-api.us-west-2.amazonaws.com/Alpha/PushNotifications/notification-preferences")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer ${MainActivity.winegardIdToken}")
            .get()
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            MainActivity.client.newCall(getNotificationRequest).execute().use { response ->
                if(!response.isSuccessful){
                    println("Response Unsuccessful: Code ${response.code}")
                    println("Response from Server: ${response.body.string()}")
                }
                else extractNotificationType(JSONObject(response.body.string()).optJSONArray("data"))

            }
        }
    }

    private fun enableNotificationType(notificationMsg: String){

        val enableNotificationJSON = JSONObject().put("notification_type", notificationMsg).toString()

        val enableNotification = Request.Builder()
            .url("https://0ehkztwewg.execute-api.us-west-2.amazonaws.com/Alpha/PushNotifications/notification-preferences")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer ${MainActivity.winegardIdToken}")
            .post(enableNotificationJSON.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        MainActivity.client.newCall(enableNotification).execute().use { response ->
            if(!response.isSuccessful){
                println("Response Unsuccessful: Error Code ${response.code}")
                println("Response from Server: ${response.body.string()}")
            }
        }
    }

    private fun disableNotificationType(notificationMsg: String){
        val enableNotificationJSON = JSONObject().put("notification_type", notificationMsg).toString()

        val disableNotification = Request.Builder()
            .url("https://0ehkztwewg.execute-api.us-west-2.amazonaws.com/Alpha/PushNotifications/notification-preferences/?notification_type=${notificationMsg}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer ${MainActivity.winegardIdToken}")
            .delete(enableNotificationJSON.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        MainActivity.client.newCall(disableNotification).execute().use { response ->
            if (!response.isSuccessful) {
                println("Response Unsuccessful: Error Code ${response.code}")
                println("Response from Server: ${response.body.string()}")
            }
        }
    }

    private fun updateRozieService(){
        val MyRozie   = 0
        val RozieCore = 1
        val Rozie2    = 2
        val Disabled  = 3

        val rozieVersion = activity.resources.getStringArray(array.RozieVersions)


        findViewById<Button>(R.id.AppConfigBtn).setOnClickListener{
            MainActivity.preferences.saveString("CoachModel", yearSpinner.getSelectedItem().toString())
            MainActivity.preferences.saveString("CoachModelName", coachModelSpinner.getSelectedItem().toString())

            if(coachModelSpinner.getSelectedItem().toString() == "Newmar"){
//                if (yearSpinner.getSelectedItem().toString().toInt() >= 2026){
//                    rozieVersionSpinner.setSelection(Rozie2)
//                    activity.getFBToken()
//                    MainActivity.preferences.saveString("RozieVersion",rozieVersion[Rozie2])
//                }
//                else{
                    rozieVersionSpinner.setSelection(MyRozie)
                    MainActivity.preferences.saveString("RozieVersion",rozieVersion[MyRozie])
//                }
            }
            else if(coachModelSpinner.getSelectedItem().toString() == "Winnebago"){
//                if (yearSpinner.getSelectedItem().toString().toInt() >= 2026){
//                    activity.getFBToken()
//                    rozieVersionSpinner.setSelection(Rozie2)
//                }
//                else{
//                    rozieVersionSpinner.setSelection(RozieCore)
//                }

                rozieVersionSpinner.setSelection(RozieCore)
                MainActivity.preferences.saveString("RozieVersion",rozieVersion[RozieCore])
            }
            else if(coachModelSpinner.getSelectedItem().toString() == "Foretravel"){
//                if (yearSpinner.getSelectedItem().toString().toInt() >= 2026){
//                    rozieVersionSpinner.setSelection(Rozie2)
//                    activity.getFBToken()
//                }
//                else{
//                    rozieVersionSpinner.setSelection(MyRozie)
//                }

                rozieVersionSpinner.setSelection(MyRozie)
                MainActivity.preferences.saveString("RozieVersion",rozieVersion[MyRozie])
            }
            else{
                rozieVersionSpinner.setSelection(MyRozie)
                MainActivity.preferences.saveString("RozieVersion",rozieVersion[MyRozie])
            }
            //yearSpinner

            //rozieVersionSpinner.setSelection(0)

            var regBtn = findViewById<Button>(R.id.RegisterBtn)
            if(rozieVersionSpinner.getSelectedItem().toString() == "Rozie 2"){

                CloudBtn.visibility = VISIBLE

                if(activity.accessKeyHasTimedOut()){
                    regBtn.visibility = GONE
                }
                else {
                    regBtn.visibility = VISIBLE
                }
            }
            else{
                CloudBtn.visibility = GONE
                regBtn.visibility = GONE
            }

        }
    }

    private fun bindUI() {

        val coachModels = activity.resources.getStringArray(array.CoachNames)

        updateRozieService()


        if(coachModelSpinner != null){
            val modelAdapter = ArrayAdapter(
                activity.applicationContext,
                R.layout.color_spinner,
                coachModels
            )

            coachModelSpinner.adapter = modelAdapter

            var currentCoachModel: String = if(MainActivity.preferences.retrieveString("CoachModelName") == null) "Newmar"
            else MainActivity.preferences.retrieveString("CoachModelName")!!

            coachModelSpinner.setSelection(modelAdapter.getPosition(currentCoachModel))
            coachModelSpinner.onItemSelectedListener = object:

            AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long){
                    MainActivity.preferences.saveString("CoachModelName", coachModels[position])
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    val nothingSelectedToast = Toast.makeText(activity.applicationContext, "No Model Selected", Toast.LENGTH_SHORT)
                    nothingSelectedToast.show()
                }
            }
        }

        val rozieVersion = activity.resources.getStringArray(array.RozieVersions)

        if(rozieVersionSpinner != null){
            val rozieAdapter = ArrayAdapter(
                activity.applicationContext,
                R.layout.color_spinner,
                rozieVersion
            )
            rozieVersionSpinner.adapter = rozieAdapter

            val currentRozieVersion: String = if(MainActivity.preferences.retrieveString("RozieVersion") == null) "None"
            else {
                MainActivity.preferences.retrieveString("RozieVersion")!!
            }

            rozieVersionSpinner.setSelection(rozieAdapter.getPosition(currentRozieVersion))

            rozieVersionSpinner.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long){
                    MainActivity.preferences.saveString("RozieVersion", rozieVersion[position])

                    if(rozieVersion[position] == "None"){
                        MainActivity.cloudServiceStatus = false
                        webView.clearCache(true)
                        saveCloudStatus(MainActivity.cloudServiceStatus)
                    }else{
                        MainActivity.cloudServiceStatus = true
                        webView.clearCache(true)
                        saveCloudStatus(MainActivity.cloudServiceStatus)
                    }

                    var regBtn = findViewById<Button>(R.id.RegisterBtn)
                    if(rozieVersion[position] == "Rozie 2"){

                        if(activity.accessKeyHasTimedOut()){
                            regBtn.visibility = GONE
                        }
                        else {
                            regBtn.visibility = VISIBLE
                        }

                        CloudBtn.visibility = VISIBLE

                        if(MainActivity.FBToken == "") {
                            activity.getFBToken()
                        }
                    }
                    else{
                        CloudBtn.visibility = GONE
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {
                    val nothingSelectedToast = Toast.makeText(activity.applicationContext, "No Model Selected", Toast.LENGTH_SHORT)
                    nothingSelectedToast.show()
                }
            }
        }

        val httpLoginStatus = activity.resources.getStringArray(array.HTTPAutoLogin)
        val httpLoginSpinner = findViewById<Spinner>(id.HTTPAutoLoginSpinner)

        if(httpLoginSpinner != null){
            val httpAdapter = ArrayAdapter(
                activity.applicationContext,
                R.layout.color_spinner,
                httpLoginStatus
            )

            httpLoginSpinner.adapter = httpAdapter

            var currentHTTPSetting: String = if(MainActivity.preferences.retrieveString("httpLoginSetting") == null ) "Auto Cloud Login Off"
            else MainActivity.preferences.retrieveString("httpLoginSetting")!!

            httpLoginSpinner.setSelection(httpAdapter.getPosition(currentHTTPSetting))
            httpLoginSpinner.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    MainActivity.preferences.saveString("httpLoginSetting", httpLoginStatus[position])
                    MainActivity.isHTTPAutoLoginEnabled = httpLoginStatus[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    val nothingSelectedToast = Toast.makeText(activity.applicationContext, "No Setting Selected", Toast.LENGTH_SHORT)
                    nothingSelectedToast.show()
                }
            }

        }

        val coachVersions = activity.resources.getStringArray(array.Versions)

        if(yearSpinner != null) {
            val adapter = ArrayAdapter(
                activity.applicationContext,
                R.layout.color_spinner,
                coachVersions
            )

            yearSpinner.adapter = adapter

            var currentPositionString: String = if(MainActivity.preferences.retrieveString("CoachModel") == null) "Newmar"
            else (MainActivity.preferences.retrieveString("CoachModel")!!)

            yearSpinner.setSelection(adapter.getPosition(currentPositionString))
//            yearSpinner.onItemSelectedListener = object:
//                AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
//                    val previousVersion = MainActivity.preferences.retrieveString("CoachModel")
//                    MainActivity.preferences.saveString("CoachModel", coachVersions[position])
//
//                    if(coachVersions[position] != previousVersion) {
//                        if(coachVersions[position].toInt() >= 2026){
//                            MainActivity.preferences.saveString("RozieVersion", "Rozie 2")
//
//                            rozieVersionSpinner.setSelection(2)
//                            CloudBtn.visibility = VISIBLE
//                            MainActivity.cloudServiceStatus = true
//                            webView.clearCache(true)
//                            saveCloudStatus(MainActivity.cloudServiceStatus)
//                        }
//                        else{
//                            MainActivity.preferences.saveString("RozieVersion", "None")
//
//                            CloudBtn.visibility = GONE
//                            rozieVersionSpinner.setSelection(3)
//
//                            MainActivity.cloudServiceStatus = false
//                            webView.clearCache(true)
//                            saveCloudStatus(MainActivity.cloudServiceStatus)
//                        }
//                    }
//                }
//
//                override fun onNothingSelected(parent: AdapterView<*>) {
//                    val nothingSelectedToast = Toast.makeText(activity.applicationContext, "No Model Selected", Toast.LENGTH_SHORT)
//                    nothingSelectedToast.show()
//                }
//            }
        }

//        smsCheckBox.setOnClickListener{
//            CoroutineScope(Dispatchers.IO).launch {
//                if (smsCheckBox.isChecked) {
//                    MainActivity.preferences.saveBoolean("areSMSNotificationsActive", true)
//                    enableNotificationType("sms")
//                } else {
//                    MainActivity.preferences.saveBoolean("areSMSNotificationsActive", false)
//                    disableNotificationType("sms")
//                }
//              //  getCurrentlyActiveNotifications()
//            }
//        }

        emailCheckBox.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                if(emailCheckBox.isChecked) {
                    MainActivity.preferences.saveBoolean("areEmailNotificationsActive", true)
                    enableNotificationType("email")
                } else {
                    MainActivity.preferences.saveBoolean("areEmailNotificationsActive", false)
                    disableNotificationType("email")
                }
              //  getCurrentlyActiveNotifications()
            }
        }

        pushCheckbox.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                if(pushCheckbox.isChecked) {
                    MainActivity.preferences.saveBoolean("arePushNotificationsActive", true)
                    enableNotificationType("push")
                } else {
                    MainActivity.preferences.saveBoolean("arePushNotificationsActive", false)
                    disableNotificationType("push")
                }
                //  getCurrentlyActiveNotifications()
            }
        }

        buttonAdvancedSettings = findViewById(id.buttonAdvancedSettings)
        buttonAdvancedSettings.setOnClickListener{

            webView.loadUrl("${getIPAddress()}/admin")
            this.cancel()
        }

        buttonConnectionSettings = findViewById(id.buttonConnectionSettings)
        buttonConnectionSettings.setOnClickListener{

            webView.loadUrl("${getIPAddress()}/network")
            this.cancel()
        }

        buttonOemSettings = findViewById(id.buttonOemSettings)
        buttonOemSettings.setOnClickListener{

            webView.loadUrl("${getIPAddress()}/oem")
            this.cancel()
        }

/**************************************************************************/


        displaySpinner = findViewById<Spinner>(id.DisplaysStaysOn)

        val displayStatus = activity.resources.getStringArray(array.DisplayShutsOff)

        if(displaySpinner != null){
            val displayAdapter = ArrayAdapter(
                activity.applicationContext,
                R.layout.color_spinner,
                displayStatus
            )

            displaySpinner.adapter = displayAdapter

            var currentDisplaySetting: Boolean = MainActivity.preferences.retrieveBoolean("screenAlwaysOnStatus")

            if(currentDisplaySetting){
                displaySpinner.setSelection(0)
            }
            else{
                displaySpinner.setSelection(1)
            }

            displaySpinner.onItemSelectedListener = object:
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    saveScreenStatus(position == 0);
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }

        }

//        enableScreenAlwaysOn = findViewById(id.enableScreenAlwaysOn)
//        enableScreenAlwaysOn.setOnClickListener {
//            MainActivity.screenAlwaysOn = true
//
//            saveScreenStatus(MainActivity.screenAlwaysOn)
//            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//
//            screenStatus = findViewById(id.screenStatus)
//            screenStatus.text = "Enabled"
//        }
//
//        disableScreenAlwaysOn = findViewById(id.disableScreenAlwaysOn)
//        disableScreenAlwaysOn.setOnClickListener{
//            MainActivity.screenAlwaysOn = false
//
//            saveScreenStatus(MainActivity.screenAlwaysOn)
//            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//
//            screenStatus = findViewById(id.screenStatus)
//            screenStatus.text = "Disabled"
//        }
//
//        /*************************************************************************/
//
//        enableCloudFeatures = findViewById(id.enableCloudFeatures)
//        enableCloudFeatures.setOnClickListener {
//
//            MainActivity.cloudServiceStatus = true
//            webView.clearCache(true)
//
//            smsCheckBox.visibility = VISIBLE
//            emailCheckBox.visibility = VISIBLE
//
//            saveCloudStatus(MainActivity.cloudServiceStatus)
//
//            cloudStatus = findViewById(id.cloudStatus)
//            cloudStatus.text = "Enabled"
//    /*
//            if ((rozieCoreServicesOverride) || (MainActivity.preferences.retrieveString("CoachModel") == "2025") || (MainActivity.preferences.retrieveString("CoachModel") == "2026")){
//                MainActivity.cloudServiceStatus = true
//            webView.clearCache(true)
//            saveCloudStatus(MainActivity.cloudServiceStatus)
//                cloudStatus = findViewById(id.cloudStatus)
//                cloudStatus.text = "Enabled"
//        }else{
//            val rozieWarningToast = Toast.makeText(activity.applicationContext, "Rozie Core Services unavailable for this coach model.", Toast.LENGTH_SHORT )
//            rozieWarningToast.show()
//            cloudStatus = findViewById(id.cloudStatus)
//            cloudStatus.text = "Disabled"
//        }
//*/
//        }
//
//        disableCloudFeatures = findViewById(id.disableCloudFeatures)
//        disableCloudFeatures.setOnClickListener{
//            MainActivity.cloudServiceStatus = false
//            webView.clearCache(true)
//
//            saveCloudStatus(MainActivity.cloudServiceStatus)
//
//            if(MainActivity.sms_id.isNotBlank())   disableNotificationType(MainActivity.sms_id)
//            if(MainActivity.email_id.isNotBlank()) disableNotificationType(MainActivity.email_id)
//
//            smsCheckBox.visibility = GONE
//            emailCheckBox.visibility = GONE
//
//            MainActivity.preferences.saveString("RozieVersion", "None")
//
//            cloudStatus = findViewById(id.cloudStatus)
//            cloudStatus.text = "Disabled"
//
//        }

        returnToAppButton = findViewById(id.returnToAppButton)
        returnToAppButton.setOnClickListener {
            this.cancel()
        }

    }
}

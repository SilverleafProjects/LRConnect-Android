package com.silverleaf.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import com.silverleaf.lrgizmo.R
import android.view.View
import android.widget.*
import com.silverleaf.lrgizmo.R.*

import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.screenAlwaysOn

private var ipAddress: String = MainActivity.ipAddress

class DialogSystemControlSettings(activity: Activity, webView: WebView): Dialog(activity) {

    private lateinit var buttonAdvancedSettings: Button
    private lateinit var buttonConnectionSettings: Button
    private lateinit var buttonOemSettings: Button

    private lateinit var enableCloudFeatures: Button
    private lateinit var disableCloudFeatures: Button
    private lateinit var disableScreenAlwaysOn: Button

    private lateinit var cloudStatus: TextView
    private lateinit var screenStatus: TextView

    private lateinit var smsCheckBox:   CheckBox
    private lateinit var emailCheckBox: CheckBox
    private lateinit var rozieVersionSpinner: Spinner

    private lateinit var enableScreenAlwaysOn: Button
    private lateinit var returnToAppButton: Button

    var rozieCoreServicesOverride: Boolean = false

    private var webView: WebView
    private var activity: Activity

    init {
        setCancelable(false)
        webView.also { this.webView = it }
        activity.also { this.activity = it }
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

        smsCheckBox = findViewById<CheckBox>(R.id.smsAlertCheckbox)
        emailCheckBox = findViewById<CheckBox>(R.id.emailAlertCheckbox)
        rozieVersionSpinner = findViewById<Spinner>(id.RozieVersionSpinner)

        if(!MainActivity.cloudServiceStatus){
            smsCheckBox.visibility = View.GONE
            emailCheckBox.visibility = View.GONE
        }

        updateCloudServiceText(MainActivity.cloudServiceStatus)
        updateScreenStatusText(MainActivity.screenAlwaysOn)
        bindUI()
    }

    private fun getIPAddress(): String {
        val ipAddress = MainActivity.ipAddress

        val protocol = context.resources.getString(string.protocol_http)
        return "$protocol$ipAddress"
    }

    private fun updateCloudServiceText(cloudservice: Boolean) {
        try {
            if (cloudservice) {
                cloudStatus = findViewById(id.cloudStatus)
                cloudStatus.text = "Enabled"
            } else {
                cloudStatus = findViewById(id.cloudStatus)
                cloudStatus.text = "Disabled"
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun updateScreenStatusText(screenstatus: Boolean) {
        try {
            if (screenstatus) {
                screenStatus = findViewById(id.screenStatus)
                screenStatus.text = "Enabled"
            } else {
                screenStatus = findViewById(id.screenStatus)
                screenStatus.text = "Disabled"
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

    }

    private fun bindUI() {

        val httpLoginStatus = activity.resources.getStringArray(array.HTTPAutoLogin)
        val httpLoginSpinner = findViewById<Spinner>(id.HTTPAutoLoginSpinner)

        if(httpLoginSpinner != null){
            val httpAdapter = ArrayAdapter(activity.applicationContext, android.R.layout.simple_spinner_item, httpLoginStatus)
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


        val rozieVersion = activity.resources.getStringArray(array.RozieVersions)

        if(rozieVersionSpinner != null){
            val rozieAdapter = ArrayAdapter(activity.applicationContext, android.R.layout.simple_spinner_item, rozieVersion)
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
                        cloudStatus = findViewById(R.id.cloudStatus)
                        cloudStatus.text = "Disabled"
                    }else{
                        MainActivity.cloudServiceStatus = true
                        webView.clearCache(true)
                        saveCloudStatus(MainActivity.cloudServiceStatus)
                        cloudStatus = findViewById(R.id.cloudStatus)
                        cloudStatus.text = "Enabled"
                    }

                    if(rozieVersion[position] == "Rozie 2"){
                        smsCheckBox.visibility = View.VISIBLE
                        emailCheckBox.visibility = View.VISIBLE
                    }else{
                        smsCheckBox.visibility = View.GONE
                        emailCheckBox.visibility = View.GONE
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {
                    val nothingSelectedToast = Toast.makeText(activity.applicationContext, "No Model Selected", Toast.LENGTH_SHORT)
                    nothingSelectedToast.show()
                }
            }
        }



        val coachVersions = activity.resources.getStringArray(array.Versions)
        val versionSpinner = findViewById<Spinner>(id.VersionSpinner)
        if(versionSpinner != null) {
            val adapter = ArrayAdapter(activity.applicationContext, android.R.layout.simple_spinner_item, coachVersions)
            versionSpinner.adapter = adapter

            var currentPositionString: String = if(MainActivity.preferences.retrieveString("CoachModel") == null)
                "2019"
            else (MainActivity.preferences.retrieveString("CoachModel")!!)

            versionSpinner.setSelection(adapter.getPosition(currentPositionString))
            versionSpinner.onItemSelectedListener = object:
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    MainActivity.preferences.saveString("CoachModel", coachVersions[position])
                    val previousVersion = MainActivity.preferences.retrieveString("CoachModel")

                    if(coachVersions[position].toString() != previousVersion.toString()) {
                        if((coachVersions[position].toString() != "2025") && (coachVersions[position].toString() != "2026"))
                        {
                            MainActivity.cloudServiceStatus = false
                            webView.clearCache(true)
                            saveCloudStatus(MainActivity.cloudServiceStatus)
                            cloudStatus = findViewById(R.id.cloudStatus)
                            cloudStatus.text = "Disabled"
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    val nothingSelectedToast = Toast.makeText(activity.applicationContext, "No Model Selected", Toast.LENGTH_SHORT)
                    nothingSelectedToast.show()
                }
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

        enableScreenAlwaysOn = findViewById(id.enableScreenAlwaysOn)
        enableScreenAlwaysOn.setOnClickListener {
            MainActivity.screenAlwaysOn = true

            saveScreenStatus(MainActivity.screenAlwaysOn)
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            screenStatus = findViewById(id.screenStatus)
            screenStatus.text = "Enabled"
        }

        disableScreenAlwaysOn = findViewById(id.disableScreenAlwaysOn)
        disableScreenAlwaysOn.setOnClickListener{
            MainActivity.screenAlwaysOn = false

            saveScreenStatus(MainActivity.screenAlwaysOn)
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            screenStatus = findViewById(id.screenStatus)
            screenStatus.text = "Disabled"
        }

        /*************************************************************************/

        enableCloudFeatures = findViewById(id.enableCloudFeatures)
        enableCloudFeatures.setOnClickListener {

            MainActivity.cloudServiceStatus = true
            webView.clearCache(true)
            saveCloudStatus(MainActivity.cloudServiceStatus)
            cloudStatus = findViewById(id.cloudStatus)
            cloudStatus.text = "Enabled"

        }

        disableCloudFeatures = findViewById(id.disableCloudFeatures)
        disableCloudFeatures.setOnClickListener{
            MainActivity.cloudServiceStatus = false
            webView.clearCache(true)

            saveCloudStatus(MainActivity.cloudServiceStatus)

            cloudStatus = findViewById(id.cloudStatus)
            cloudStatus.text = "Disabled"

        }

        returnToAppButton = findViewById(id.returnToAppButton)
        returnToAppButton.setOnClickListener {
            this.cancel()
        }

    }
}

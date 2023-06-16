package com.silverleaf.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
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

    private lateinit var enableScreenAlwaysOn: Button
    private lateinit var returnToAppButton: Button

    private var webView: WebView
    private var activity: Activity

    private fun callScreenStatusToggle(activity: MainActivity) = activity.toggleScreenStatus(screenAlwaysOn)

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
        setContentView(R.layout.dialog_advanced_settings)

        updateCloudServiceText(MainActivity.cloudServiceStatus)
        updateScreenStatusText(MainActivity.screenAlwaysOn)
        bindUI()
    }

    private fun getIPAddress(): String {
        val ipAddress = MainActivity.ipAddress

        val protocol = context.resources.getString(R.string.protocol_http)
        return "$protocol$ipAddress"
    }

    private fun updateCloudServiceText(cloudservice: Boolean) {
        try {
            if (cloudservice) {
                cloudStatus = findViewById(R.id.cloudStatus)
                cloudStatus.text = "Enabled"
            } else {
                cloudStatus = findViewById(R.id.cloudStatus)
                cloudStatus.text = "Disabled"
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun updateScreenStatusText(screenstatus: Boolean) {
        try {
            if (screenstatus) {
                screenStatus = findViewById(R.id.screenStatus)
                screenStatus.text = "Enabled"
            } else {
                screenStatus = findViewById(R.id.screenStatus)
                screenStatus.text = "Disabled"
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

    }

    private fun bindUI() {

        buttonAdvancedSettings = findViewById(R.id.buttonAdvancedSettings)
        buttonAdvancedSettings.setOnClickListener{

            webView.loadUrl("${getIPAddress()}/admin")
            this.cancel()
        }

        buttonConnectionSettings = findViewById(R.id.buttonConnectionSettings)
        buttonConnectionSettings.setOnClickListener{

            webView.loadUrl("${getIPAddress()}/network")
            this.cancel()
        }

        buttonOemSettings = findViewById(R.id.buttonOemSettings)
        buttonOemSettings.setOnClickListener{

            webView.loadUrl("${getIPAddress()}/oem")
            this.cancel()
        }

/**************************************************************************/

        enableScreenAlwaysOn = findViewById(R.id.enableScreenAlwaysOn)
        enableScreenAlwaysOn.setOnClickListener {
            MainActivity.screenAlwaysOn = true

            saveScreenStatus(MainActivity.screenAlwaysOn)
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            screenStatus = findViewById(R.id.screenStatus)
            screenStatus.text = "Enabled"
        }

        disableScreenAlwaysOn = findViewById(R.id.disableScreenAlwaysOn)
        disableScreenAlwaysOn.setOnClickListener{
            MainActivity.screenAlwaysOn = false

            saveScreenStatus(MainActivity.screenAlwaysOn)
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            screenStatus = findViewById(R.id.screenStatus)
            screenStatus.text = "Disabled"
        }

        /*************************************************************************/

        enableCloudFeatures = findViewById(R.id.enableCloudFeatures)
        enableCloudFeatures.setOnClickListener{

            MainActivity.cloudServiceStatus = true
            webView.clearCache(true)

            saveCloudStatus(MainActivity.cloudServiceStatus)

            cloudStatus = findViewById(R.id.cloudStatus)
            cloudStatus.text = "Enabled"

        }

        disableCloudFeatures = findViewById(R.id.disableCloudFeatures)
        disableCloudFeatures.setOnClickListener{
            MainActivity.cloudServiceStatus = false
            webView.clearCache(true)

            saveCloudStatus(MainActivity.cloudServiceStatus)

            cloudStatus = findViewById(R.id.cloudStatus)
            cloudStatus.text = "Disabled"

        }

        returnToAppButton = findViewById(R.id.returnToAppButton)
        returnToAppButton.setOnClickListener {
            this.cancel()
        }

    }
}

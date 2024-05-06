package com.silverleaf.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.Window
import android.webkit.WebView
import android.widget.*
import com.silverleaf.lrgizmo.R
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.*
import com.silverleaf.lrgizmo.R.*

class DialogYear(activity: Activity): Dialog(activity) {

    private var activity: Activity

    lateinit var buttonDialogDismiss: Button
    private lateinit var rozieVersionSpinner: Spinner

    init {
        setCancelable(false)
        activity.also { this.activity = it }
    }

    fun saveCloudStatus(token: Boolean) {
        MainActivity.preferences.saveBoolean("cloudServiceStatus", token)
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(layout.dialog_year)

        bindUI()
    }

    private fun bindUI() {

        val coachVersions = activity.resources.getStringArray(R.array.Versions)
        val versionSpinner = findViewById<Spinner>(R.id.CoachVersionStartupSpinner)

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
                            saveCloudStatus(MainActivity.cloudServiceStatus)
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    val nothingSelectedToast = Toast.makeText(activity.applicationContext, "No Model Selected", Toast.LENGTH_SHORT)
                    nothingSelectedToast.show()
                }
            }
        }

        val automaticLogin = activity.resources.getStringArray(R.array.HTTPAutoLogin)
        val autologinSpinner = findViewById<Spinner>(R.id.OnStartupAutomaticLogin)

        if(autologinSpinner != null) {
            val adapter = ArrayAdapter(activity.applicationContext, android.R.layout.simple_spinner_item, automaticLogin)
            autologinSpinner.adapter = adapter

            var currentPositionString: String = if(MainActivity.preferences.retrieveString("CoachModel") == null)
                "2019"
            else (MainActivity.preferences.retrieveString("CoachModel")!!)

            autologinSpinner.setSelection(adapter.getPosition(currentPositionString))
            autologinSpinner.onItemSelectedListener = object:
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    MainActivity.preferences.saveString("httpLoginSetting", automaticLogin[position])
                    MainActivity.isHTTPAutoLoginEnabled = automaticLogin[position]

                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    val nothingSelectedToast = Toast.makeText(activity.applicationContext, "No Model Selected", Toast.LENGTH_SHORT)
                    nothingSelectedToast.show()
                }
            }
        }

        val rozieVersion = activity.resources.getStringArray(array.RozieVersions)
        val rozieVersionSpinner = findViewById<Spinner>(id.RozieVersionSpinner)
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
                        saveCloudStatus(MainActivity.cloudServiceStatus)
                    }else{
                        MainActivity.cloudServiceStatus = true
                        saveCloudStatus(MainActivity.cloudServiceStatus)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {
                    val nothingSelectedToast = Toast.makeText(activity.applicationContext, "No Model Selected", Toast.LENGTH_SHORT)
                    nothingSelectedToast.show()
                }
            }
        }

        buttonDialogDismiss = findViewById(R.id.buttonDialogAcceptSettings)
        buttonDialogDismiss.setOnClickListener {
            MainActivity.userHasSelectedValues = true
            MainActivity.preferences.saveBoolean("HasUserSelectedCoachModel", true)
            this.cancel()
        }

    }

}
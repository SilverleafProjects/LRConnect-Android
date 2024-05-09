package com.silverleaf.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
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

class DialogModelAndYear(activity: Activity): Dialog(activity) {

    private var activity: Activity

    lateinit var buttonDialogDismiss: Button
    lateinit var buttonDialogWebService: Button

    init {
        setCancelable(false)
        activity.also { this.activity = it }
    }

    private fun configureRozieSettingsByCoachModel(coachmodel: String, modelyear: String){
        if(modelyear != "Other" && modelyear.toInt() == 2026) {
            when (coachmodel) {
                "Winnebago" -> MainActivity.preferences.saveString("RozieVersion", "Rozie Core Services")
                "Newmar" -> MainActivity.preferences.saveString("RozieVersion", "Rozie 2")
                else -> MainActivity.preferences.saveString("RozieVersion", "MyRozie")
            }
        }else{
            when(coachmodel) {
                "Winnebago" -> MainActivity.preferences.saveString("RozieVersion", "None")
                "Newmar" -> MainActivity.preferences.saveString("RozieVersion", "RozieCoreServices")
                else -> MainActivity.preferences.saveString("RozieVersion", "MyRozie")
            }
        }
    }

    fun saveCloudStatus(token: Boolean) {
        MainActivity.preferences.saveBoolean("cloudServiceStatus", token)
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(layout.dialog_model_and_year)
        bindUI()
    }

    private fun bindUI() {

        val coachModels = activity.resources.getStringArray(array.CoachNames)
        val coachModelSpinner = findViewById<Spinner>(id.CoachModelStartupSpinner)

        if(coachModelSpinner != null){
            val modelAdapter = ArrayAdapter(activity.applicationContext, R.layout.color_spinner, coachModels)
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

        val coachVersions = activity.resources.getStringArray(R.array.Versions)
        val versionSpinner = findViewById<Spinner>(R.id.CoachVersionStartupSpinner)

        if(versionSpinner != null) {
            val adapter = ArrayAdapter(activity.applicationContext, R.layout.color_spinner, coachVersions)
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

//        buttonDialogWebService = findViewById(R.id.buttonEnableWebService)
//        buttonDialogWebService.setOnClickListener {
//
//
//                MainActivity.preferences.retrieveString("CoachModel")
//                    ?.let { it1 ->
//                        configureRozieSettingsByCoachModel(
//                            MainActivity.preferences.retrieveString(
//                                "CoachModelName"
//                            )!!, it1
//                        )
//                    }
//                MainActivity.preferences.saveBoolean("HasUserSelectedCoachModel", true)
//                MainActivity.preferences.saveBoolean("cloudServiceStatus", true)
//                MainActivity.goToCloudLogin = true
//                this.cancel()
//
//        }

        buttonDialogDismiss = findViewById(R.id.buttonDialogAcceptSettings)
        buttonDialogDismiss.setOnClickListener {

            MainActivity.preferences.retrieveString("CoachModel")
                ?.let { it1 ->
                    configureRozieSettingsByCoachModel(
                        MainActivity.preferences.retrieveString(
                            "CoachModelName"
                        )!!, it1
                    )
                }
            MainActivity.preferences.saveBoolean("HasUserSelectedCoachModel", true)
            MainActivity.preferences.saveBoolean("cloudServiceStatus", false)
            this.cancel()

        }

    }

}
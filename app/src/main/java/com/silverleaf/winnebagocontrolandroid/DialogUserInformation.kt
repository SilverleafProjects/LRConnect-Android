package com.silverleaf.winnebagocontrolandroid

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.Window
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.silverleaf.lrgizmo.R
import preferences.Preferences
import android.view.View
import com.silverleaf.lrgizmo.R.*
import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.httpAccessToken
import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.tokenValidStartTime
import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.winegardAccessToken
import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.winegardIdToken
import com.silverleaf.winnebagocontrolandroid.MainActivity.Companion.winegardRefreshToken
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException


class DialogUserInformation(activity: MainActivity, webView: WebView) : Dialog(activity) {
    private lateinit var buttonDialogEnterInformationAccept: Button
    private lateinit var buttonDialogEnterIPAddressCancel: Button
    private lateinit var editTextDialogEnterEmail: EditText
    private lateinit var editTextDialogEnterPassword: EditText
    private lateinit var enteredPassword: String
    private lateinit var enteredEmail: String
    private lateinit var enteredCredentials: TextView
    private var connectionFailureCount: Int = 0
    private var preferences = Preferences(activity.baseContext)
    var dialogSide: Int = 0

    private var webView: WebView

    private var activity: MainActivity
    init {
        setCancelable(false)
        activity.also { this.activity = it }
    }

    init {
        setCancelable(false)
        webView.also { this.webView = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_enter_user_information)
        bindUI()
    }

    private fun createWinegardJSONMessage(): String {
        var winegardJSONMessage: JSONObject = JSONObject()
        winegardJSONMessage.put("grant_type", "client_credentials")
        winegardJSONMessage.put("client_id", "6n6sbmrqdn053d78858310974d")
        winegardJSONMessage.put("client_secret", "10kmr0bbckpr0c8tb7hmglmqgh270mte2b1nohk5ggi0boej94v9")
        return winegardJSONMessage.toString()
    }

    private fun createHTTPUserLoginMessage(email: String, password: String): String {
        var httpUserLoginMessage: JSONObject = JSONObject()
        httpUserLoginMessage.put("client_secret", "10kmr0bbckpr0c8tb7hmglmqgh270mte2b1nohk5ggi0boej94v9")
        httpUserLoginMessage.put("email"   ,    email)
        httpUserLoginMessage.put("password", password)
//        httpUserLoginMessage.put("email"   ,    "joshua@simply-smarter.com")
//        httpUserLoginMessage.put("password", "@1862Civilwar")

        return httpUserLoginMessage.toString()
    }

    //"application/json; charset=utf-8".toMediaType()
    private fun runHttpMessage() {
            try {
                val request = Request.Builder()
                    .url("https://identity.winegard-staging.io/api/v1/oauth2/token")
                    .post(createWinegardJSONMessage().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                MainActivity.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful){
                        println("Error Code: ${response.code}")
                        runHttpMessage()
                    }
                    val httpResponse = response.body.string() //${JSONObject(httpResponse).get("token_type")}
                    httpAccessToken = ("Bearer ${JSONObject(httpResponse).get("access_token")}")
                    MainActivity.preferences.saveInt("AccessTimeout", JSONObject(httpResponse).getInt("expires_in"))
                    tokenValidStartTime = System.currentTimeMillis()
                    MainActivity.preferences.saveLong("TokenStartTime", tokenValidStartTime)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
    }

    private fun transmitUserData(email: String, password: String)
    {
        val httpAccessCoroutine: Deferred<String> = CoroutineScope(Dispatchers.IO).async{
            runHttpMessage()
            httpAccessToken
        }

        MainActivity.broadcastMessageCoroutine.launch {
            try {
                    val request = Request.Builder()
                        .url("https://identity.winegard-staging.io/api/v1/auth/login")
                        .header("Content-Type", "application/json")
                        .header("Authorization", httpAccessCoroutine.await())
                        .post(createHTTPUserLoginMessage(email, password).toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()

                    MainActivity.client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful){
                            if(connectionFailureCount == 3){
                                MainActivity.isConnectedToCloud = false
                                throw IOException("Response Unsuccessful!")
                            }
                            connectionFailureCount++
                            transmitUserData(email, password)
                        }
                        val fullResponseString = response.body.string()

                        winegardAccessToken = JSONObject(fullResponseString).get("access_token").toString()
                        winegardIdToken = JSONObject(fullResponseString).get("id_token").toString()
                        winegardRefreshToken = JSONObject(fullResponseString).get("refresh_token").toString()

                        MainActivity.preferences.saveString("AccessToken", winegardAccessToken)
                        MainActivity.preferences.saveString("IDToken", winegardIdToken)
                        MainActivity.preferences.saveString("RefreshToken", winegardRefreshToken)

                        if(MainActivity.updatedToken){
                            connectionFailureCount = 0
                            activity.updatePushToken()
                        }
                    }

                yield()
            } catch(e: Exception){
                e.printStackTrace()
                this.cancel()
                yield()
            }
            loadRozieCoreServices(winegardAccessToken, winegardIdToken, winegardRefreshToken)
        }
    }

    private fun loadRozieCoreServices(accesstoken: String, idtoken: String, refreshtoken: String){
        if(accesstoken.isEmpty() || idtoken.isEmpty() || refreshtoken.isEmpty()) {
            this.cancel()
        }
        else{
                webView.post(Runnable {
                    webView.loadUrl("https://www.roziecoreservices.com/rozie2?accessToken=${accesstoken}&idToken=${idtoken}&refreshToken=${refreshtoken}")
                })
            }
        }

    private fun bindUI() {

        editTextDialogEnterEmail = findViewById(id.editTextDialogEnterEmail)
        enteredEmail = editTextDialogEnterEmail.text.toString()

        editTextDialogEnterPassword = findViewById(id.editTextDialogEnterPassword)
        enteredPassword = editTextDialogEnterPassword.text.toString()

        buttonDialogEnterIPAddressCancel = findViewById(id.buttonDialogConfirmInformation)
        buttonDialogEnterIPAddressCancel.setOnClickListener{

            if(editTextDialogEnterEmail.text.toString().isEmpty() && editTextDialogEnterPassword.text.toString().isEmpty()){
                enteredCredentials = findViewById(id.testEmailPasswordOrBothIncorrect)
                enteredCredentials.text = "Enter valid user credentials."
                enteredCredentials.visibility = View.VISIBLE
            }else if(editTextDialogEnterPassword.text.toString().isEmpty()){
                enteredCredentials = findViewById(id.testEmailPasswordOrBothIncorrect)
                enteredCredentials.text = "Enter a valid password."
                enteredCredentials.visibility = View.VISIBLE
            }else if(editTextDialogEnterEmail.text.toString().isEmpty()){
                enteredCredentials = findViewById(id.testEmailPasswordOrBothIncorrect)
                enteredCredentials.text = "Enter a valid email address."
                enteredCredentials.visibility = View.VISIBLE
            }else{
                transmitUserData(editTextDialogEnterEmail.text.toString(), editTextDialogEnterPassword.text.toString())
                MainActivity.userEnteredCredentials = true
                this.cancel()
            }

        }

        buttonDialogEnterIPAddressCancel = findViewById(id.buttonDialogEnterIPAddressCancel)
        buttonDialogEnterIPAddressCancel.setOnClickListener {
            this.cancel()
        }

    }
}

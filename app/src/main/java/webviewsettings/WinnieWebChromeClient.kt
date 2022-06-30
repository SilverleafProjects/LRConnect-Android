package webviewsettings

import android.app.Activity
import android.net.Uri
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import com.example.winnebagocontrolandroid.MainActivity
import com.example.winnebagocontrolandroid.MainActivity.Companion.FILE_RESULT_CODE
import java.lang.Exception

class WinnieWebChromeClient(progressBar: ProgressBar, activity: Activity): WebChromeClient() {
    var progressBar: ProgressBar = progressBar
    var activity: Activity = activity

    override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
        if(MainActivity.valueCallBack != null) {
            MainActivity.valueCallBack!!.onReceiveValue(null)
            MainActivity.valueCallBack = null
        }
        MainActivity.valueCallBack = filePathCallback
        val intent = fileChooserParams?.createIntent()
        try {
            activity.startActivityForResult(intent, FILE_RESULT_CODE)
        }
        catch (e: Exception) {
            MainActivity.valueCallBack = null
            return false
        }
        return true
    }

    override fun onProgressChanged(view: WebView?, progress: Int) {
        if(progress > 99) {
            progressBar.visibility = View.GONE
            return
        }
        progressBar.visibility = View.VISIBLE
        progressBar.progress = progress

        super.onProgressChanged(view, progress)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if(consoleMessage?.message()!!.contains("myWonderfulToken")) {
            val messageParts = consoleMessage.message().split(":")
            MainActivity.saveTokenToPreferences(messageParts[1])
            println("My token: ${messageParts[1]}")
        }
        return super.onConsoleMessage(consoleMessage)
    }


}
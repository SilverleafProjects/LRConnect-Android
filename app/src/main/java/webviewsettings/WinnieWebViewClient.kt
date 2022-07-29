package webviewsettings

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.webkit.*
import androidx.annotation.RequiresApi

class WinnieWebViewClient : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        println("Started loading Page: $url")
        super.onPageStarted(view, url, favicon)
    }
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        println(request.toString())
        return false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        val errorCode = error!!.errorCode
        println("Request: ${request.toString()}")
        println("Error: $errorCode")
        super.onReceivedError(view, request, error)
    }

    /** TODO: Use this so the user doesn't have to remember the web page token
    override fun onPageFinished(view: WebView?, url: String?) {
        if(url!!.contains("testmyrozie")) {
            println("testmyrozie Loaded.")
            view!!.loadUrl("javascript:(function() { setTimeout(function() { console.log('myWonderfulToken:' + document.getElementById('registration_token').innerHTML) }, 1000)})()")
        }
        super.onPageFinished(view, url)
    }
    **/
    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
//        super.onReceivedSslError(view, handler, error)
        val sslCertificate = error!!.certificate.toString()
        println("SSL: $sslCertificate")

        if(sslCertificate.contains("Silverleaf")) {
            handler!!.proceed()
        }
    }

}
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
//        return super.shouldOverrideUrlLoading(view, request)
        return false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        val errorCode = error!!.errorCode
        println("Request: ${request.toString()}")
        println("Error: $errorCode")
        super.onReceivedError(view, request, error)
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
//        super.onReceivedSslError(view, handler, error)
        val sslCertificate = error!!.certificate.toString()
        println("SSL: $sslCertificate")
        handler!!.proceed()
    }

}
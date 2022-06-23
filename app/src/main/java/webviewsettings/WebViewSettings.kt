package webviewsettings

import android.annotation.SuppressLint
import android.app.Activity
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar

@SuppressLint("SetJavaScriptEnabled")
fun setWebView(webView: WebView, progressBar: ProgressBar, activity: Activity) {
    val settings = webView.settings
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.loadsImagesAutomatically = true
    settings.allowFileAccess = true
    settings.allowContentAccess = true

    webView.isVerticalScrollBarEnabled = false
    webView.isHorizontalScrollBarEnabled = false


//    var webChromeClient = WebChromeClient()
//    webView.webChromeClient = webChromeClient
    webView.webViewClient = WinnieWebViewClient()
    webView.webChromeClient = WinnieWebChromeClient(progressBar, activity)
}



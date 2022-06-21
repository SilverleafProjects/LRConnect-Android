package webviewsettings

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar

@SuppressLint("SetJavaScriptEnabled")
fun setWebView(webView: WebView, progressBar: ProgressBar) {
    val settings = webView.settings
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.loadsImagesAutomatically = true

    webView.isVerticalScrollBarEnabled = false
    webView.isHorizontalScrollBarEnabled = false


//    var webChromeClient = WebChromeClient()
//    webView.webChromeClient = webChromeClient
    webView.webViewClient = WinnieWebViewClient()
    webView.webChromeClient = WinnieWebChromeClient(progressBar)
}



package webviewsettings

import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar

class WinnieWebChromeClient(progressBar: ProgressBar): WebChromeClient() {
    var progressBar: ProgressBar = progressBar

    //    override fun onShowFileChooser(
//        webView: WebView?,
//        filePathCallback: ValueCallback<Array<Uri>>?,
//        fileChooserParams: FileChooserParams?
//    ): Boolean {
//        return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
//    }

    override fun onProgressChanged(view: WebView?, progress: Int) {
        if(progress > 99) {
            progressBar.visibility = View.GONE
            return
        }
        progressBar.visibility = View.VISIBLE
        progressBar.progress = progress

        super.onProgressChanged(view, progress)
    }

}
package com.silverleaf.winnebagocontrolandroid

import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class LifecycleListener(webView: WebView) : DefaultLifecycleObserver{
        private var webView: WebView
        init {
            webView.also{ this.webView = it }
        }

        override fun onStart(owner: LifecycleOwner) {
            Log.d("SampleLifecycle", "Returning to foreground…")
            webView.post(Runnable {
                webView.loadUrl("javascript:appEvent('InForeground')")
            })
        }

        override fun onStop(owner: LifecycleOwner) {
            Log.d("SampleLifecycle", "Moving to background…")
            webView.post(Runnable {
                webView.loadUrl("javascript:appEvent('InBackground')")
            })
        }
}
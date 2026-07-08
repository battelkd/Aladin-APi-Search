package com.jxino.aladinaccessiblebookapp.web

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.jxino.aladinaccessiblebookapp.domain.WebPageAssistant

class BookWebViewController(
    private val pageAssistant: WebPageAssistant,
    private val onLoadingChanged: (Boolean) -> Unit,
) {
    fun attach(webView: WebView) {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                return if (uri.scheme == "http" || uri.scheme == "https") {
                    false
                } else {
                    runCatching {
                        view.context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                    true
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                onLoadingChanged(true)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                pageAssistant.analyzePageState(url.orEmpty(), view.title)
                onLoadingChanged(false)
            }
        }
    }

    fun load(webView: WebView, url: String) {
        webView.loadUrl(url)
    }

    fun canGoBack(webView: WebView?): Boolean = webView?.canGoBack() == true

    fun goBack(webView: WebView?) {
        webView?.goBack()
    }
}

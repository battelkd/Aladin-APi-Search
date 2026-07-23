package com.jxino.aladinaccessiblebookapp.web

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.jxino.aladinaccessiblebookapp.domain.CartActionResult
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

    fun clickCartButton(webView: WebView?, onResult: (CartActionResult) -> Unit) {
        val view = webView ?: run {
            onResult(CartActionResult.NotProductPage)
            return
        }
        val currentUrl = view.url.orEmpty()
        if (!currentUrl.isAladinProductPage()) {
            onResult(CartActionResult.NotProductPage)
            return
        }

        view.evaluateJavascript(CLICK_CART_BUTTON_SCRIPT) { rawResult ->
            val result = rawResult
                ?.trim()
                ?.removeSurrounding("\"")
                ?.lowercase()
            onResult(
                if (result == "clicked") {
                    CartActionResult.Added
                } else {
                    CartActionResult.ButtonMissing
                },
            )
        }
    }

    private fun String.isAladinProductPage(): Boolean =
        runCatching {
            val uri = Uri.parse(this)
            val host = uri.host.orEmpty().lowercase()
            val path = uri.path.orEmpty().lowercase()
            val query = uri.query.orEmpty().lowercase()
            host.endsWith("aladin.co.kr") &&
                query.contains("itemid=") &&
                (path.contains("wproduct.aspx") || path.contains("mproduct.aspx") || path.contains("product"))
        }.getOrDefault(false)

    private companion object {
        val CLICK_CART_BUTTON_SCRIPT = """
            (function() {
                try {
                    const absoluteHref = function(element) {
                        const href = element && element.getAttribute ? element.getAttribute('href') : '';
                        if (!href) return '';
                        try {
                            return new URL(href, window.location.href).href;
                        } catch (error) {
                            return href;
                        }
                    };
                    const addBookFromHref = function(href) {
                        if (!href) return '';
                        const match = href.match(/[?&]AddBook=([^&#]+)/i);
                        return match ? decodeURIComponent(match[1]) : '';
                    };
                    const isEbookAddBook = function(value) {
                        return /^E[a-z0-9]+$/i.test(String(value || '').trim());
                    };
                    const ebookIsbnFromPage = function() {
                        const hrefIsbns = Array.from(document.querySelectorAll('a[href*="AddBook="]'))
                            .map(function(anchor) { return addBookFromHref(absoluteHref(anchor)); })
                            .filter(isEbookAddBook);
                        if (hrefIsbns.length > 0) return hrefIsbns[0];

                        const hiddenIsbns = Array.from(document.querySelectorAll('input.hd_ISBN, input[id*="hd_ISBN"], input[name*="hd_ISBN"]'))
                            .map(function(input) { return input.value; })
                            .filter(isEbookAddBook);
                        return hiddenIsbns[0] || '';
                    };
                    const forceEbookIsbn = function(ebookIsbn) {
                        Array.from(document.querySelectorAll('input.hd_ISBN, input[id*="hd_ISBN"], input[name*="hd_ISBN"]')).forEach(function(input) {
                            input.value = ebookIsbn;
                        });
                    };
                    const ebookIsbn = ebookIsbnFromPage();
                    if (!ebookIsbn) return 'button_not_found';
                    forceEbookIsbn(ebookIsbn);

                    // The visible mobile cart button calls CrossBasketAdd_Layer().
                    // On mixed paper/eBook Aladin pages that function reads hd_RelationISBN
                    // and adds the paper book, so eBook cart actions must use hd_ISBN/AddBook=E...
                    if (typeof window.BasketAdd_Layer === 'function') {
                        window.BasketAdd_Layer();
                        return 'clicked';
                    }

                    if (typeof window.fn_addbasket_Product_v2 === 'function') {
                        window.fn_addbasket_Product_v2(ebookIsbn, 'False', 'False');
                        return 'clicked';
                    }

                    const exactEbookAddHref = Array.from(document.querySelectorAll('a[href*="AddBook="]'))
                        .map(function(anchor) { return absoluteHref(anchor); })
                        .find(function(href) {
                            return addBookFromHref(href).toLowerCase() === ebookIsbn.toLowerCase();
                        });
                    if (exactEbookAddHref) {
                        window.location.href = exactEbookAddHref;
                        return 'clicked';
                    }

                    window.location.href = new URL(
                        '/shop/wbasket.aspx?AddBook=' + encodeURIComponent(ebookIsbn),
                        window.location.href
                    ).href;
                    return 'clicked';
                } catch (error) {
                    return 'button_not_found';
                }
            })();
        """.trimIndent()
    }
}

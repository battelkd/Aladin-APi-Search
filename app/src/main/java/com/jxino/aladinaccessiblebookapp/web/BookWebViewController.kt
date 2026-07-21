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
                    const clickableSelector = 'a, button, input, [role="button"], area';
                    const elementSelector = 'a, button, input, [role="button"], area, img, span, div';
                    const cartWords = ['장바구니', '바구니', '카트', 'cart', 'basket'];
                    const addWords = ['담기', '담아', '넣기', '넣어', '추가', 'add', 'insert'];
                    const strongWords = [
                        '장바구니에담기',
                        '장바구니담기',
                        '장바구니에넣기',
                        '장바구니넣기',
                        '바구니에담기',
                        '바구니담기',
                        '카트에담기',
                        'addtocart',
                        'addbasket'
                    ];
                    const negativeWords = [
                        '장바구니보기',
                        '장바구니로이동',
                        '바구니보기',
                        '내역',
                        '삭제',
                        '비우기',
                        '주문',
                        '결제'
                    ];
                    const codeCartWords = [
                        'addbasket',
                        'basketadd',
                        'basket_add',
                        'addcart',
                        'cartadd',
                        'add_cart'
                    ];

                    const normalize = function(value) {
                        return String(value || '')
                            .toLowerCase()
                            .replace(/\s+/g, '')
                            .trim();
                    };
                    const attributeText = function(element) {
                        if (!element) return '';
                        const attrs = [
                            element.innerText,
                            element.textContent,
                            element.value,
                            element.title,
                            element.alt,
                            element.href,
                            element.getAttribute && element.getAttribute('aria-label'),
                            element.getAttribute && element.getAttribute('onclick'),
                            element.getAttribute && element.getAttribute('class'),
                            element.getAttribute && element.getAttribute('id')
                        ];
                        const childImages = element.querySelectorAll ? Array.from(element.querySelectorAll('img')) : [];
                        childImages.forEach(function(image) {
                            attrs.push(image.alt, image.title);
                        });
                        return attrs.filter(Boolean).join(' ');
                    };
                    const visible = function(element) {
                        if (!element) return false;
                        const style = window.getComputedStyle(element);
                        if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') {
                            return false;
                        }
                        return element.getClientRects().length > 0;
                    };
                    const clickableOf = function(element) {
                        if (!element) return null;
                        if (element.matches && element.matches(clickableSelector)) return element;
                        return element.closest ? element.closest(clickableSelector) : null;
                    };

                    const candidates = [];
                    Array.from(document.querySelectorAll(elementSelector)).forEach(function(element) {
                        const clickable = clickableOf(element);
                        if (!clickable || !visible(clickable)) return;

                        const text = normalize(attributeText(element) + ' ' + attributeText(clickable));
                        if (!text) return;
                        if (negativeWords.some(function(word) { return text.indexOf(normalize(word)) >= 0; })) return;

                        const strongMatch = strongWords.some(function(word) { return text.indexOf(normalize(word)) >= 0; });
                        const hasCartWord = cartWords.some(function(word) { return text.indexOf(normalize(word)) >= 0; });
                        const hasAddWord = addWords.some(function(word) { return text.indexOf(normalize(word)) >= 0; });
                        const hasCartCode = codeCartWords.some(function(word) { return text.indexOf(word) >= 0; });

                        if (strongMatch || (hasCartWord && hasAddWord) || hasCartCode) {
                            let score = 0;
                            if (strongMatch) score += 30;
                            if (hasCartWord && hasAddWord) score += 20;
                            if (hasCartCode) score += 15;
                            if (clickable.tagName === 'BUTTON' || clickable.tagName === 'INPUT') score += 3;
                            candidates.push({ element: clickable, score: score });
                        }
                    });

                    candidates.sort(function(a, b) { return b.score - a.score; });
                    const target = candidates.length > 0 ? candidates[0].element : null;
                    if (!target) return 'button_not_found';

                    target.scrollIntoView({ block: 'center', inline: 'center' });
                    target.click();
                    return 'clicked';
                } catch (error) {
                    return 'button_not_found';
                }
            })();
        """.trimIndent()
    }
}

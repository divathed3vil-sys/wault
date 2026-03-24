// android/app/src/main/kotlin/com/diva/wault/WaultWebViewClient.kt

package com.diva.wault

import android.content.Intent
import android.net.Uri
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class WaultWebViewClient(
    private val onPageFinishedCallback: ((String) -> Unit)? = null,
    private val onRenderProcessGoneCallback: (() -> Unit)? = null
) : WebViewClient() {

    companion object {
        private val ALLOWED_HOSTS = setOf(
            "web.whatsapp.com",
            "whatsapp.com",
            "www.whatsapp.com",
            "wa.me",
            "static.whatsapp.net",
            "mmg.whatsapp.net",
            "pps.whatsapp.net",
            "media.whatsapp.net"
        )
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val url = request?.url?.toString() ?: return false
        return handleUrlLoading(view, url)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url == null) return false
        return handleUrlLoading(view, url)
    }

    private fun handleUrlLoading(view: WebView?, url: String): Boolean {
        val uri = try {
            Uri.parse(url)
        } catch (_: Exception) {
            return true
        }

        val host = uri.host?.lowercase() ?: return true

        val isAllowed = ALLOWED_HOSTS.any { allowedHost ->
            host == allowedHost || host.endsWith(".$allowedHost")
        }

        if (isAllowed) {
            return false
        }

        val context = view?.context ?: return true
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }

        return true
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        if (view != null) {
            injectCss(view)
            injectJs(view)
        }

        if (url != null) {
            onPageFinishedCallback?.invoke(url)
        }
    }

    override fun onRenderProcessGone(
        view: WebView?,
        detail: RenderProcessGoneDetail?
    ): Boolean {
        onRenderProcessGoneCallback?.invoke()
        view?.destroy()
        return true
    }

    private fun injectCss(webView: WebView) {
        val css = """
            (function() {
              try {
                var existing = document.getElementById('wault-native-feel');
                if (existing) existing.remove();

                var style = document.createElement('style');
                style.id = 'wault-native-feel';
                style.textContent = `
                  html, body {
                    background: #0b141a !important;
                    overscroll-behavior: none !important;
                  }
                  body {
                    -webkit-user-select: none !important;
                    user-select: none !important;
                    -webkit-touch-callout: none !important;
                  }
                  ::-webkit-scrollbar {
                    width: 3px;
                    height: 3px;
                  }
                  ::-webkit-scrollbar-thumb {
                    background: rgba(255,255,255,0.2);
                    border-radius: 999px;
                  }
                `;
                document.head.appendChild(style);
              } catch (e) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(css, null)
    }

    private fun injectJs(webView: WebView) {
        val js = """
            (function() {
              try {
                document.addEventListener('contextmenu', function(e) {
                  e.preventDefault();
                }, true);

                document.addEventListener('dragstart', function(e) {
                  e.preventDefault();
                }, true);

                setInterval(function() {
                  try {
                    var unreadNodes = document.querySelectorAll('[data-testid="icon-unread-count"]');
                    var total = 0;
                    unreadNodes.forEach(function(node) {
                      var value = parseInt((node.textContent || '0').trim(), 10);
                      if (!isNaN(value)) total += value;
                    });
                    if (window.WaultBridge && window.WaultBridge.onUnreadCount) {
                      window.WaultBridge.onUnreadCount(total);
                    }
                  } catch (e) {}
                }, 3000);

                setInterval(function() {
                  try {
                    var qrVisible = document.querySelector('[data-testid="qrcode"]') != null;
                    var loggedIn = document.querySelector('#pane-side') != null;

                    if (qrVisible && window.WaultBridge && window.WaultBridge.onQRVisible) {
                      window.WaultBridge.onQRVisible();
                    } else if (loggedIn && window.WaultBridge && window.WaultBridge.onLoggedIn) {
                      window.WaultBridge.onLoggedIn();
                    }
                  } catch (e) {}
                }, 2000);
              } catch (e) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }
}
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
}
package com.diva.wault

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView

class WaultChromeClient(
    private val onConsoleMessageCallback: ((String) -> Unit)? = null
) : WebChromeClient() {

    companion object {
        private const val TAG = "WaultChromeClient"
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        request?.grant(request.resources)
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        result?.confirm()
        return true
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (consoleMessage == null) return false

        val msg = "[${consoleMessage.messageLevel()}] " +
                "${consoleMessage.sourceId()}:${consoleMessage.lineNumber()} - " +
                consoleMessage.message()

        when (consoleMessage.messageLevel()) {
            ConsoleMessage.MessageLevel.ERROR -> Log.e(TAG, msg)
            ConsoleMessage.MessageLevel.WARNING -> Log.w(TAG, msg)
            else -> Log.d(TAG, msg)
        }

        onConsoleMessageCallback?.invoke(msg)

        return true
    }
}
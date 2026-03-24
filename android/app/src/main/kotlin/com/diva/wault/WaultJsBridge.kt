// android/app/src/main/kotlin/com/diva/wault/WaultJsBridge.kt

package com.diva.wault

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

class WaultJsBridge(
    private val accountId: String,
    private val onUnreadCountChanged: ((String, Int) -> Unit)? = null,
    private val onQrVisible: ((String) -> Unit)? = null,
    private val onLoggedInState: ((String) -> Unit)? = null
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onUnreadCount(count: Int) {
        mainHandler.post {
            onUnreadCountChanged?.invoke(accountId, count)
        }
    }

    @JavascriptInterface
    fun onQRVisible() {
        mainHandler.post {
            onQrVisible?.invoke(accountId)
        }
    }

    @JavascriptInterface
    fun onLoggedIn() {
        mainHandler.post {
            onLoggedInState?.invoke(accountId)
        }
    }
}
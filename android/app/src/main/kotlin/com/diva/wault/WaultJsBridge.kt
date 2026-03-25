// File: android/app/src/main/kotlin/com/diva/wault/WaultJsBridge.kt
package com.diva.wault

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

class WaultJsBridge(
    private val applicationContext: Context,
    private val accountId: String
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onUnreadCount(count: Int) {
        mainHandler.post {
            EventBroadcaster.sendUnreadCount(
                context = applicationContext,
                accountId = accountId,
                count = count
            )
        }
    }

    @JavascriptInterface
    fun onQRVisible() {
        mainHandler.post {
            EventBroadcaster.sendQrVisible(
                context = applicationContext,
                accountId = accountId
            )
        }
    }

    @JavascriptInterface
    fun onLoggedIn() {
        mainHandler.post {
            EventBroadcaster.sendLoggedIn(
                context = applicationContext,
                accountId = accountId
            )
        }
    }
}
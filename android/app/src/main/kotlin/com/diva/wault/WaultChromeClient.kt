// File: android/app/src/main/kotlin/com/diva/wault/WaultChromeClient.kt
package com.diva.wault

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.content.ContextCompat

class WaultChromeClient(
    private val activity: BaseWebViewSessionActivity
) : WebChromeClient() {

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<android.net.Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        filePathCallback?.onReceiveValue(null)
        return false
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        if (request == null) {
            return
        }

        activity.runOnUiThread {
            try {
                val requestedResources = request.resources ?: emptyArray()
                val grantedResources = mutableListOf<String>()

                for (resource in requestedResources) {
                    when (resource) {
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                            if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
                                grantedResources.add(resource)
                            }
                        }

                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                            if (hasPermission(Manifest.permission.CAMERA)) {
                                grantedResources.add(resource)
                            }
                        }

                        PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> {
                            grantedResources.add(resource)
                        }
                    }
                }

                if (grantedResources.isNotEmpty()) {
                    request.grant(grantedResources.toTypedArray())
                } else {
                    request.deny()
                }
            } catch (_: Exception) {
                try {
                    request.deny()
                } catch (_: Exception) {
                }
            }
        }
    }

    @SuppressLint("WebChromeClientOnGeolocationPermissionsShowPrompt")
    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        callback?.invoke(origin, false, false)
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        result?.cancel()
        return true
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        return true
    }

    private fun hasPermission(permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(activity, permission) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
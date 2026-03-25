// File: android/app/src/main/kotlin/com/diva/wault/WaultChromeClient.kt
package com.diva.wault

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class WaultChromeClient(
    private val activity: BaseWebViewSessionActivity
) : WebChromeClient() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = filePathCallback
            filePathCallback = null

            if (callback == null) {
                return@registerForActivityResult
            }

            if (result.resultCode != Activity.RESULT_OK) {
                callback.onReceiveValue(null)
                return@registerForActivityResult
            }

            val data = result.data
            val uris = mutableListOf<Uri>()

            val clipData = data?.clipData
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i)?.uri?.let { uris.add(it) }
                }
            } else {
                data?.data?.let { uris.add(it) }
            }

            if (uris.isEmpty()) {
                callback.onReceiveValue(null)
            } else {
                callback.onReceiveValue(uris.toTypedArray())
            }
        }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback

        return try {
            val acceptTypes = fileChooserParams?.acceptTypes
                ?.filter { it.isNotBlank() }
                ?.toTypedArray()
                ?: emptyArray()

            val mimeTypes = if (acceptTypes.isNotEmpty()) acceptTypes else arrayOf("*/*")

            val contentIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = if (mimeTypes.size == 1) mimeTypes.first() else "*/*"
                putExtra(
                    Intent.EXTRA_ALLOW_MULTIPLE,
                    fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                )
                if (mimeTypes.size > 1) {
                    putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                }
            }

            val chooserIntent = Intent(Intent.ACTION_CHOOSER).apply {
                putExtra(Intent.EXTRA_INTENT, contentIntent)
                putExtra(Intent.EXTRA_TITLE, "Select file")
            }

            fileChooserLauncher.launch(chooserIntent)
            true
        } catch (_: Exception) {
            this.filePathCallback = null
            false
        }
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
// android/app/src/main/kotlin/com/diva/wault/BaseWebViewSessionActivity.kt

package com.diva.wault

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView

abstract class BaseWebViewSessionActivity : Activity() {

    abstract val slotNumber: Int

    private var webView: WebView? = null
    private var titleView: TextView? = null
    private var controlReceiver: BroadcastReceiver? = null

    private var accountId: String = ""
    private var accountLabel: String = ""
    private var accentColor: String = ""

    companion object {
        const val EXTRA_ACCOUNT_ID = "accountId"
        const val EXTRA_LABEL = "label"
        const val EXTRA_ACCENT_COLOR = "accentColor"
        const val EXTRA_PROCESS_SLOT = "processSlot"

        private const val WHATSAPP_WEB_URL = "https://web.whatsapp.com"
        private const val BACKGROUND_COLOR = "#0A0A0F"
        private const val TOP_BAR_COLOR = "#1A1A2E"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        readIntentExtras()
        setupDataDirectory()

        val root = buildLayout()
        setContentView(root)

        setupWebView()
        registerControlReceiver()
        loadInitialUrl()
    }

    private fun readIntentExtras() {
        accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID) ?: ""
        accountLabel = intent.getStringExtra(EXTRA_LABEL) ?: "Session"
        accentColor = intent.getStringExtra(EXTRA_ACCENT_COLOR) ?: ""
    }

    private fun setupDataDirectory() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                WebView.setDataDirectorySuffix("wault_$slotNumber")
            } catch (_: IllegalStateException) {
            } catch (_: Exception) {
            }
        }
    }

    private fun buildLayout(): LinearLayout {
        val density = resources.displayMetrics.density
        val topBarHeight = (48 * density).toInt()
        val buttonSize = (36 * density).toInt()
        val horizontalPadding = (12 * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor(BACKGROUND_COLOR))
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                topBarHeight
            )
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            setBackgroundColor(Color.parseColor(TOP_BAR_COLOR))
        }

        val backButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_revert)
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.WHITE)
            setOnClickListener { handleBackPressed() }
            layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize)
        }

        val title = TextView(this).apply {
            text = accountLabel
            setTextColor(Color.WHITE)
            textSize = 16f
            maxLines = 1
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val closeButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.WHITE)
            setOnClickListener { finishSession() }
            layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize)
        }

        topBar.addView(backButton)
        topBar.addView(title)
        topBar.addView(closeButton)

        val web = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setBackgroundColor(Color.parseColor(BACKGROUND_COLOR))
        }

        titleView = title
        webView = web

        root.addView(topBar)
        root.addView(web)

        return root
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val wv = webView ?: return

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            allowFileAccess = false
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
        }

        wv.overScrollMode = WebView.OVER_SCROLL_NEVER
        wv.isVerticalScrollBarEnabled = false
        wv.isHorizontalScrollBarEnabled = false
        wv.isHapticFeedbackEnabled = false
        wv.setOnLongClickListener { true }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
        }

        val jsBridge = WaultJsBridge(
            accountId = accountId,
            onUnreadCountChanged = { id, count ->
                EventBroadcaster.sendUnreadCount(this, id, count)
            },
            onQrVisible = { id ->
                EventBroadcaster.sendQrVisible(this, id)
                EventBroadcaster.sendSessionStateChanged(this, id, "COLD")
            },
            onLoggedInState = { id ->
                EventBroadcaster.sendLoggedIn(this, id)
                EventBroadcaster.sendSessionStateChanged(this, id, "ACTIVE")
            }
        )

        wv.addJavascriptInterface(jsBridge, "WaultBridge")

        wv.webViewClient = WaultWebViewClient(
            onPageFinishedCallback = { _ -> },
            onRenderProcessGoneCallback = {
                EventBroadcaster.sendSessionCrashed(this, accountId)
                EventBroadcaster.sendSessionError(this, accountId, "Render process gone")
            }
        )

        wv.webChromeClient = WaultChromeClient()
    }

    private fun registerControlReceiver() {
        if (controlReceiver != null) return

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                if (intent.action != EventBroadcaster.ACTION) return

                val type = intent.getStringExtra(EventBroadcaster.KEY_TYPE) ?: return
                val targetAccountId = intent.getStringExtra(EventBroadcaster.KEY_ACCOUNT_ID) ?: ""

                when (type) {
                    EventBroadcaster.TYPE_CONTROL_CLOSE_ALL -> finishSession()
                    EventBroadcaster.TYPE_CONTROL_CLOSE_SESSION -> {
                        if (targetAccountId == accountId) {
                            finishSession()
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(EventBroadcaster.ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }

        controlReceiver = receiver
    }

    private fun loadInitialUrl() {
        webView?.loadUrl(WHATSAPP_WEB_URL)
    }

    private fun handleBackPressed() {
        val wv = webView
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            finishSession()
        }
    }

    private fun finishSession() {
        EventBroadcaster.sendSessionStateChanged(this, accountId, "COLD")
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        handleBackPressed()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        controlReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {
            }
        }
        controlReceiver = null

        webView?.apply {
            stopLoading()
            loadUrl("about:blank")
            removeAllViews()
            destroy()
        }
        webView = null
        titleView = null
        super.onDestroy()
    }
}
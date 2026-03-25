// File: android/app/src/main/kotlin/com/diva/wault/BaseWebViewSessionActivity.kt
package com.diva.wault

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

abstract class BaseWebViewSessionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ACCOUNT_ID = "accountId"
        const val EXTRA_LABEL = "label"
        const val EXTRA_ACCENT_COLOR = "accentColor"
        const val EXTRA_PROCESS_SLOT = "processSlot"

        private const val WHATSAPP_URL = "https://web.whatsapp.com"
        private const val DEFAULT_ACCENT = "#25D366"
        private var dataDirectorySuffixApplied = false
    }

    protected abstract val slotNumber: Int

    private lateinit var accountId: String
    private lateinit var accountLabel: String
    private lateinit var accentColorHex: String
    private var processSlot: Int = -1

    private lateinit var rootLayout: FrameLayout
    private lateinit var sessionContainer: LinearLayout
    private lateinit var topBar: LinearLayout
    private lateinit var webViewContainer: FrameLayout
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var errorOverlay: FrameLayout

    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        applyWebViewDataDirectorySuffixIfNeeded()
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID).orEmpty()
        accountLabel = intent.getStringExtra(EXTRA_LABEL).orEmpty().ifBlank { "WAult" }
        accentColorHex = intent.getStringExtra(EXTRA_ACCENT_COLOR).orEmpty().ifBlank { DEFAULT_ACCENT }
        processSlot = intent.getIntExtra(EXTRA_PROCESS_SLOT, slotNumber)

        buildLayout()
        createAndAttachWebView()
        setupBackHandling()

        EventBroadcaster.sendSessionStateChanged(
            context = applicationContext,
            accountId = accountId,
            state = "ACTIVE"
        )
    }

    private fun applyWebViewDataDirectorySuffixIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !dataDirectorySuffixApplied) {
            try {
                WebView.setDataDirectorySuffix("wault_$slotNumber")
                dataDirectorySuffixApplied = true
            } catch (_: IllegalStateException) {
            } catch (_: Exception) {
            }
        }
    }

    private fun buildLayout() {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#0F0F14")

        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0B141A"))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        sessionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B141A"))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        topBar = buildTopBar()

        webViewContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0B141A"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        loadingOverlay = buildLoadingOverlay()
        errorOverlay = buildErrorOverlay().apply {
            visibility = View.GONE
        }

        webViewContainer.addView(loadingOverlay)
        rootLayout.addView(sessionContainer)
        rootLayout.addView(errorOverlay)

        sessionContainer.addView(topBar)
        sessionContainer.addView(webViewContainer)

        setContentView(rootLayout)
    }

    private fun buildTopBar(): LinearLayout {
        val accentColor = safeParseColor(accentColorHex, DEFAULT_ACCENT)

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#F20B141A"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            )
            setPadding(dp(8), 0, dp(12), 0)

            val backButton = ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_media_previous)
                setBackgroundColor(Color.TRANSPARENT)
                setColorFilter(Color.parseColor("#E6FFFFFF"))
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
                contentDescription = "Back"
                setOnClickListener {
                    handleBackPress()
                }
            }

            val accountPill = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(6), dp(12), dp(6))
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = dpF(18)
                    setColor(adjustAlpha(accentColor, 0.16f))
                    setStroke(dp(1), adjustAlpha(accentColor, 0.35f))
                }

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginStart = dp(8)
                }

                val accentDot = View(context).apply {
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(accentColor)
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply {
                        marginEnd = dp(8)
                    }
                }

                val labelView = TextView(context).apply {
                    text = accountLabel
                    setTextColor(Color.parseColor("#E6FFFFFF"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setTypeface(typeface, Typeface.SEMI_BOLD)
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }

                addView(accentDot)
                addView(labelView)
            }

            addView(backButton)
            addView(accountPill)
        }
    }

    private fun buildLoadingOverlay(): FrameLayout {
        return FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0B141A"))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            val progress = ProgressBar(context).apply {
                isIndeterminate = true
                layoutParams = FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER)
            }

            addView(progress)
        }
    }

    private fun buildErrorOverlay(): FrameLayout {
        return FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#F20B141A"))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
                setPadding(dp(24), dp(24), dp(24), dp(24))
            }

            val title = TextView(context).apply {
                text = "Session interrupted"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
            }

            val subtitle = TextView(context).apply {
                text = "The session needs to be rebuilt."
                setTextColor(Color.parseColor("#B3FFFFFF"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(20))
            }

            val buttonsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            val retryButton = TextView(context).apply {
                text = "Retry"
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.SEMI_BOLD)
                setPadding(dp(20), dp(12), dp(20), dp(12))
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = dpF(14)
                    setColor(safeParseColor(accentColorHex, DEFAULT_ACCENT))
                }
                setOnClickListener {
                    recreateSession()
                }
            }

            val closeButton = TextView(context).apply {
                text = "Close"
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#E6FFFFFF"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.SEMI_BOLD)
                setPadding(dp(20), dp(12), dp(20), dp(12))
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = dpF(14)
                    setColor(Color.parseColor("#1FFFFFFF"))
                    setStroke(dp(1), Color.parseColor("#33FFFFFF"))
                }
                setOnClickListener {
                    finish()
                }
            }

            buttonsRow.addView(
                retryButton,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dp(10)
                }
            )

            buttonsRow.addView(closeButton)

            content.addView(title)
            content.addView(subtitle)
            content.addView(buttonsRow)

            addView(content)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createAndAttachWebView() {
        destroyCurrentWebView()

        loadingOverlay.visibility = View.VISIBLE
        errorOverlay.visibility = View.GONE

        val createdWebView = WebView(this)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(createdWebView, true)

        createdWebView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        createdWebView.setBackgroundColor(Color.parseColor("#0B141A"))
        createdWebView.isVerticalScrollBarEnabled = false
        createdWebView.isHorizontalScrollBarEnabled = false
        createdWebView.isScrollbarFadingEnabled = true
        createdWebView.overScrollMode = View.OVER_SCROLL_NEVER
        createdWebView.isLongClickable = false
        createdWebView.isHapticFeedbackEnabled = false
        createdWebView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            createdWebView.isForceDarkAllowed = false
        }

        val settings = createdWebView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadsImagesAutomatically = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.userAgentString =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        val jsBridge = WaultJsBridge(
            applicationContext = applicationContext,
            accountId = accountId
        )

        createdWebView.addJavascriptInterface(jsBridge, "WaultBridge")

        createdWebView.webViewClient = WaultWebViewClient(
            activity = this,
            accountId = accountId,
            accentColorHex = accentColorHex
        )

        createdWebView.webChromeClient = WaultChromeClient(this)

        createdWebView.setDownloadListener(
            DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                handleDownload(
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType
                )
            }
        )

        webViewContainer.removeAllViews()
        webViewContainer.addView(createdWebView)
        webViewContainer.addView(loadingOverlay)

        webView = createdWebView
        createdWebView.loadUrl(WHATSAPP_URL)
    }

    private fun handleDownload(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        if (url.isNullOrBlank()) return

        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent ?: "")
                setDescription("Downloading from WAult")
                setTitle(
                    URLUtil.guessFileName(
                        url,
                        contentDisposition,
                        mimeType
                    )
                )
                allowScanningByMediaScanner()
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimeType)
                )

                val cookies = CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrBlank()) {
                    addRequestHeader("Cookie", cookies)
                }
            }

            val downloadManager =
                getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

            if (downloadManager != null) {
                downloadManager.enqueue(request)
            } else {
                openUrlExternally(url)
            }
        } catch (_: Exception) {
            openUrlExternally(url)
        }
    }

    private fun openUrlExternally(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
        }
    }

    fun handleRendererCrash() {
        EventBroadcaster.sendSessionStateChanged(
            context = applicationContext,
            accountId = accountId,
            state = "ERROR"
        )
        showErrorOverlay("Renderer process crashed.")
    }

    fun onPageReady() {
        loadingOverlay.visibility = View.GONE
    }

    fun showErrorOverlay(message: String? = null) {
        loadingOverlay.visibility = View.GONE
        errorOverlay.visibility = View.VISIBLE

        if (!message.isNullOrBlank()) {
            EventBroadcaster.sendSessionError(
                context = applicationContext,
                accountId = accountId,
                message = message
            )
        }
    }

    private fun recreateSession() {
        EventBroadcaster.sendSessionStateChanged(
            context = applicationContext,
            accountId = accountId,
            state = "ACTIVE"
        )
        createAndAttachWebView()
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    private fun handleBackPress() {
        val currentWebView = webView
        if (currentWebView != null && currentWebView.canGoBack()) {
            currentWebView.goBack()
        } else {
            finish()
        }
    }

    private fun destroyCurrentWebView() {
        webView?.let { oldWebView ->
            try {
                oldWebView.stopLoading()
            } catch (_: Exception) {
            }
            try {
                oldWebView.loadUrl("about:blank")
            } catch (_: Exception) {
            }
            try {
                oldWebView.clearHistory()
            } catch (_: Exception) {
            }
            try {
                oldWebView.removeJavascriptInterface("WaultBridge")
            } catch (_: Exception) {
            }
            try {
                oldWebView.webChromeClient = WebChromeClient()
                oldWebView.webViewClient = WebViewClient()
            } catch (_: Exception) {
            }
            try {
                (oldWebView.parent as? ViewGroup)?.removeView(oldWebView)
            } catch (_: Exception) {
            }
            try {
                oldWebView.destroy()
            } catch (_: Exception) {
            }
        }
        webView = null
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        EventBroadcaster.sendSessionStateChanged(
            context = applicationContext,
            accountId = accountId,
            state = "ACTIVE"
        )
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        destroyCurrentWebView()
        super.onDestroy()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun dpF(value: Int): Float {
        return value * resources.displayMetrics.density
    }

    private fun safeParseColor(value: String, fallback: String): Int {
        return try {
            Color.parseColor(value)
        } catch (_: Exception) {
            Color.parseColor(fallback)
        }
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
package com.diva.wault

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

abstract class BaseWebViewSessionActivity : AppCompatActivity() {

    abstract val slotNumber: Int

    protected var webView: WebView? = null
    private var titleText: TextView? = null

    private var accountId: String = ""
    private var label: String = ""
    private var accentColor: String = ""

    companion object {
        const val EXTRA_ACCOUNT_ID = "accountId"
        const val EXTRA_LABEL = "label"
        const val EXTRA_ACCENT_COLOR = "accentColor"
        const val EXTRA_PROCESS_SLOT = "processSlot"

        private const val WHATSAPP_WEB_URL = "https://web.whatsapp.com"

        private const val TOP_BAR_HEIGHT_DP = 48
        private const val TOP_BAR_COLOR = 0xFF111B21.toInt()
        private const val BACKGROUND_COLOR = 0xFF0A0A0A.toInt()
        private const val TEXT_COLOR = 0xFFF5F5F5.toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyDataDirectorySuffix()
        readIntentExtras()
        applyFullscreen()

        val root = buildLayout()
        setContentView(root)

        setupBackHandler()
    }

    private fun applyDataDirectorySuffix() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                WebView.setDataDirectorySuffix("wault_$slotNumber")
            } catch (_: Exception) {
                // Ignore safely
            }
        }
    }

    private fun readIntentExtras() {
        accountId = intent?.getStringExtra(EXTRA_ACCOUNT_ID) ?: ""
        label = intent?.getStringExtra(EXTRA_LABEL) ?: "Session $slotNumber"
        accentColor = intent?.getStringExtra(EXTRA_ACCENT_COLOR) ?: ""
    }

    private fun applyFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = BACKGROUND_COLOR

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildLayout(): ViewGroup {
        val density = resources.displayMetrics.density
        val topBarHeightPx = (TOP_BAR_HEIGHT_DP * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BACKGROUND_COLOR)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val topBar = buildTopBar(topBarHeightPx, density)
        root.addView(topBar)

        val webContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setBackgroundColor(BACKGROUND_COLOR)
        }

        val wv = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            setBackgroundColor(BACKGROUND_COLOR)

            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportMultipleWindows(false)
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = false
                allowContentAccess = false
                userAgentString = buildDesktopUserAgent(userAgentString)
            }
        }

        webContainer.addView(wv)
        root.addView(webContainer)

        webView = wv
        wv.loadUrl(WHATSAPP_WEB_URL)

        return root
    }

    private fun buildTopBar(heightPx: Int, density: Float): LinearLayout {
        val paddingH = (12 * density).toInt()
        val buttonSize = (36 * density).toInt()

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(TOP_BAR_COLOR)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                heightPx
            )
            setPadding(paddingH, 0, paddingH, 0)
        }

        val backButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(TEXT_COLOR)
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize)
            contentDescription = "Close session"
            setOnClickListener { finish() }
        }

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                (8 * density).toInt(),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val title = TextView(this).apply {
            text = label
            setTextColor(TEXT_COLOR)
            textSize = 16f
            isSingleLine = true
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        }

        topBar.addView(backButton)
        topBar.addView(spacer)
        topBar.addView(title)

        titleText = title
        return topBar
    }

    private fun buildDesktopUserAgent(defaultAgent: String): String {
        val chromeVersion =
            Regex("Chrome/[\\d.]+").find(defaultAgent)?.value ?: "Chrome/120.0.0.0"

        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) $chromeVersion Safari/537.36"
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView?.let {
                    if (it.canGoBack()) it.goBack()
                    else finish()
                }
            }
        })
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
        webView?.let { wv ->
            wv.stopLoading()
            wv.loadUrl("about:blank")
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.removeAllViews()
            wv.destroy()
        }
        webView = null
        titleText = null
        super.onDestroy()
    }
}
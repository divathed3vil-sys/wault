// File: android/app/src/main/kotlin/com/diva/wault/WaultWebViewClient.kt
package com.diva.wault

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class WaultWebViewClient(
    private val activity: BaseWebViewSessionActivity,
    private val accountId: String,
    private val accentColorHex: String
) : WebViewClient() {

    private val allowedDomains = listOf(
        "web.whatsapp.com",
        "whatsapp.com",
        "wa.me",
        "static.whatsapp.net",
        "mmg.whatsapp.net",
        "pps.whatsapp.net",
        "media.whatsapp.net"
    )

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val uri = request?.url ?: return false
        val host = uri.host?.lowercase() ?: return false

        val isAllowed = allowedDomains.any { domain ->
            host == domain || host.endsWith(".$domain")
        }

        return if (isAllowed) {
            false
        } else {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(intent)
            } catch (_: Exception) {
            }
            true
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        EventBroadcaster.sendSessionStateChanged(
            context = activity.applicationContext,
            accountId = accountId,
            state = "ACTIVE"
        )
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)

        injectWaultNativeShell(view)
        injectFocusedSessionBehavior(view)
        injectStateObservers(view)
        injectQrAwareLayoutLogic(view)

        activity.onPageReady()
    }

    override fun onRenderProcessGone(
        view: WebView,
        detail: RenderProcessGoneDetail
    ): Boolean {
        EventBroadcaster.sendSessionCrashed(
            context = activity.applicationContext,
            accountId = accountId,
            didCrash = detail.didCrash()
        )
        activity.handleRendererCrash()
        return true
    }

    private fun injectWaultNativeShell(webView: WebView) {
        val safeAccent = accentColorHex.ifBlank { "#25D366" }

        val js = """
            (function() {
              try {
                var existing = document.getElementById('wault-native-feel');
                if (existing) existing.remove();

                var style = document.createElement('style');
                style.id = 'wault-native-feel';
                style.textContent = `
                  :root {
                    --wault-bg: #0b141a;
                    --wault-surface: #111b21;
                    --wault-surface-2: #16232b;
                    --wault-surface-3: #1f2c34;
                    --wault-text: #e9edef;
                    --wault-text-secondary: #aebac1;
                    --wault-accent: ${safeAccent};
                    --wault-top-offset: 48px;
                  }

                  html, body {
                    background: var(--wault-bg) !important;
                    overscroll-behavior: none !important;
                    -webkit-tap-highlight-color: transparent !important;
                    scroll-behavior: smooth !important;
                  }

                  body {
                    color: var(--wault-text) !important;
                  }

                  body, div, span, button, a, input, textarea {
                    -webkit-tap-highlight-color: transparent !important;
                  }

                  html, body, #app {
                    overscroll-behavior-y: none !important;
                    overscroll-behavior-x: none !important;
                    -webkit-overflow-scrolling: touch !important;
                    background: var(--wault-bg) !important;
                  }

                  * {
                    scrollbar-width: thin !important;
                    scrollbar-color: rgba(255,255,255,0.20) transparent !important;
                  }

                  *::-webkit-scrollbar {
                    width: 3px !important;
                    height: 3px !important;
                  }

                  *::-webkit-scrollbar-track {
                    background: transparent !important;
                  }

                  *::-webkit-scrollbar-thumb {
                    background: rgba(255,255,255,0.20) !important;
                    border-radius: 999px !important;
                  }

                  body:not([contenteditable="true"]),
                  body *:not(input):not(textarea):not([contenteditable="true"]) {
                    -webkit-user-select: none !important;
                    user-select: none !important;
                  }

                  input, textarea, [contenteditable="true"] {
                    -webkit-user-select: text !important;
                    user-select: text !important;
                  }

                  #app {
                    background: var(--wault-bg) !important;
                  }

                  #app::before {
                    content: "";
                    position: fixed;
                    top: 0;
                    left: 0;
                    right: 0;
                    height: 2px;
                    background: linear-gradient(
                      90deg,
                      transparent,
                      var(--wault-accent),
                      transparent
                    );
                    z-index: 2147483646;
                    pointer-events: none;
                  }

                  body.wault-session-active #app,
                  body.wault-session-active {
                    background: var(--wault-bg) !important;
                  }

                  body.wault-chat-mode #app {
                    padding-top: 0 !important;
                  }

                  body.wault-chat-mode header,
                  body.wault-chat-mode [data-testid="chat-list-header"],
                  body.wault-chat-mode [data-testid="chat-header"] {
                    backdrop-filter: none !important;
                  }

                  [data-testid="chat-list-search"],
                  [data-testid="search"] {
                    background: rgba(255,255,255,0.06) !important;
                    border-radius: 14px !important;
                    border: 1px solid rgba(255,255,255,0.07) !important;
                    box-shadow: none !important;
                  }

                  [data-testid="conversation-compose-box-input"] {
                    background: transparent !important;
                    color: var(--wault-text) !important;
                  }

                  [data-testid="conversation-compose-box-input"]::placeholder {
                    color: var(--wault-text-secondary) !important;
                  }

                  [data-testid="send-btn"],
                  [data-testid="audio-record-btn"],
                  [data-testid="attach-btn"],
                  [data-testid="emoji-btn"] {
                    filter: saturate(1.05);
                  }

                  [data-testid="unread-count"],
                  [data-testid="icon-unread-count"] {
                    border-radius: 999px !important;
                    min-width: 18px !important;
                    padding-left: 4px !important;
                    padding-right: 4px !important;
                  }

                  [data-testid="cell-frame-container"] {
                    border-radius: 14px !important;
                    margin-left: 6px !important;
                    margin-right: 6px !important;
                  }

                  [data-testid="cell-frame-container"]:hover {
                    background: rgba(255,255,255,0.04) !important;
                  }

                  [data-testid="conversation-panel-wrapper"],
                  #main {
                    background: var(--wault-bg) !important;
                  }

                  [data-testid="chat-list"] {
                    background: #0f171d !important;
                  }

                  [data-testid="chat-list-header"],
                  header {
                    background: #111b21 !important;
                    border-bottom: 1px solid rgba(255,255,255,0.04) !important;
                    box-shadow: none !important;
                  }

                  footer {
                    background: #111b21 !important;
                    border-top: 1px solid rgba(255,255,255,0.04) !important;
                  }

                  [data-testid="msg-container"] .message-in,
                  [data-testid="msg-container"] .message-out {
                    border-radius: 14px !important;
                    box-shadow: none !important;
                  }

                  [data-testid="msg-container"] .message-in {
                    background: #182229 !important;
                  }

                  [data-testid="msg-container"] .message-out {
                    background: #144d37 !important;
                  }

                  [data-testid="balloon-text-content"] {
                    color: var(--wault-text) !important;
                  }

                  [data-testid="msg-meta"] span {
                    color: rgba(255,255,255,0.65) !important;
                  }

                  [data-testid="notification-box"],
                  [data-testid="msg-date-divider"],
                  [data-testid="unread-messages-divider"] {
                    backdrop-filter: none !important;
                    border-radius: 12px !important;
                  }

                  [data-testid="download-mobile-app-banner"],
                  [data-testid="wa-web-banner"],
                  [data-testid="alert-phone"],
                  [data-testid="alert-computer"] {
                    display: none !important;
                  }

                  [data-testid="menu-bar-profile-photo"],
                  [data-testid="menu-bar-new-chat-icon"],
                  [data-testid="menu-bar-communities-icon"],
                  [data-testid="menu-bar-status-icon"],
                  [data-testid="menu-bar-channels-icon"],
                  [data-testid="menu-bar-menu-icon"] {
                    border-radius: 12px !important;
                  }

                  [data-testid="menu-bar-communities-icon"],
                  [data-testid="menu-bar-status-icon"],
                  [data-testid="menu-bar-channels-icon"] {
                    opacity: 0.95 !important;
                  }

                  [data-testid="call-main-btn"],
                  [data-testid="call-video-btn"],
                  [data-testid="search-toolbar-button"],
                  [data-testid="chat-settings-button"] {
                    border-radius: 10px !important;
                  }

                  #startup,
                  #landing {
                    background: var(--wault-bg) !important;
                  }

                  body.wault-qr-mode #app,
                  body.wault-qr-mode,
                  body.wault-qr-mode #landing,
                  body.wault-qr-mode #startup {
                    background: var(--wault-bg) !important;
                  }

                  body.wault-qr-mode [data-testid="qrcode"] {
                    transform: scale(0.98);
                    transform-origin: center center;
                  }
                `;
                document.head.appendChild(style);

                document.body.classList.add('wault-session-active');
              } catch (e) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    private fun injectFocusedSessionBehavior(webView: WebView) {
        val js = """
            (function() {
              try {
                if (window.__waultBehaviorInstalled) return;
                window.__waultBehaviorInstalled = true;

                document.addEventListener('contextmenu', function(e) {
                  e.preventDefault();
                }, true);

                document.addEventListener('dragstart', function(e) {
                  e.preventDefault();
                }, true);

                var lastTouchEnd = 0;
                document.addEventListener('touchend', function(e) {
                  var now = Date.now();
                  if (now - lastTouchEnd <= 300) {
                    e.preventDefault();
                  }
                  lastTouchEnd = now;
                }, { passive: false });

                document.addEventListener('touchstart', function(e) {
                  if (e.touches && e.touches.length > 1) {
                    e.preventDefault();
                  }
                }, { passive: false });

                var touchStartY = 0;
                document.addEventListener('touchstart', function(e) {
                  if (e.touches && e.touches.length > 0) {
                    touchStartY = e.touches[0].clientY;
                  }
                }, { passive: true });

                document.addEventListener('touchmove', function(e) {
                  if (!e.touches || e.touches.length === 0) return;
                  var currentY = e.touches[0].clientY;
                  var pullingDown = currentY > touchStartY;
                  var atTop = (window.scrollY || document.documentElement.scrollTop || 0) <= 0;
                  if (pullingDown && atTop) {
                    e.preventDefault();
                  }
                }, { passive: false });
              } catch (e) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    private fun injectStateObservers(webView: WebView) {
        val js = """
            (function() {
              try {
                if (window.__waultStateObserversInstalled) return;
                window.__waultStateObserversInstalled = true;

                function sendUnreadCount() {
                  try {
                    var total = 0;
                    var nodes = document.querySelectorAll(
                      '[data-testid="icon-unread-count"], [data-testid="unread-count"]'
                    );
                    nodes.forEach(function(node) {
                      var raw = (node.textContent || '').trim().replace('+', '');
                      var num = parseInt(raw, 10);
                      if (!isNaN(num)) total += num;
                    });
                    if (window.WaultBridge && window.WaultBridge.onUnreadCount) {
                      window.WaultBridge.onUnreadCount(total);
                    }
                  } catch (e) {}
                }

                function detectState() {
                  try {
                    var qr = document.querySelector('[data-testid="qrcode"]');
                    var paneSide = document.querySelector('#pane-side');

                    if (qr && window.WaultBridge && window.WaultBridge.onQRVisible) {
                      window.WaultBridge.onQRVisible();
                    } else if (paneSide && window.WaultBridge && window.WaultBridge.onLoggedIn) {
                      window.WaultBridge.onLoggedIn();
                    }
                  } catch (e) {}
                }

                sendUnreadCount();
                detectState();

                setInterval(sendUnreadCount, 3000);
                setInterval(detectState, 2000);
              } catch (e) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    private fun injectQrAwareLayoutLogic(webView: WebView) {
        val js = """
            (function() {
              try {
                if (window.__waultQrAwareInstalled) return;
                window.__waultQrAwareInstalled = true;

                function updateModeClasses() {
                  try {
                    var body = document.body;
                    if (!body) return;

                    var qr = document.querySelector('[data-testid="qrcode"]');
                    var paneSide = document.querySelector('#pane-side');
                    var chatOpen = document.querySelector('#main');

                    body.classList.remove('wault-qr-mode');
                    body.classList.remove('wault-chat-mode');

                    if (qr) {
                      body.classList.add('wault-qr-mode');
                    } else if (paneSide || chatOpen) {
                      body.classList.add('wault-chat-mode');
                    }
                  } catch (e) {}
                }

                updateModeClasses();

                var observer = new MutationObserver(function() {
                  updateModeClasses();
                });

                observer.observe(document.documentElement || document.body, {
                  childList: true,
                  subtree: true,
                  attributes: false
                });
              } catch (e) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }
}
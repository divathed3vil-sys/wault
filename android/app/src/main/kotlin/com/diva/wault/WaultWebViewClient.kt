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
        if (view != null) {
            injectViewportMeta(view)
        }
        EventBroadcaster.sendSessionStateChanged(
            context = activity.applicationContext,
            accountId = accountId,
            state = "ACTIVE"
        )
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        injectViewportMeta(view)
        injectWaultMobileShell(view)
        injectWaultPanelController(view)
        injectFocusedSessionBehavior(view)
        injectStateObservers(view)
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

    private fun injectViewportMeta(webView: WebView) {
        val js = """
            (function() {
                try {
                    var vp = document.querySelector('meta[name="viewport"]');
                    if (vp) {
                        vp.setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover');
                    } else {
                        vp = document.createElement('meta');
                        vp.name = 'viewport';
                        vp.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover';
                        if (document.head) document.head.appendChild(vp);
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun injectWaultMobileShell(webView: WebView) {
        val safeAccent = accentColorHex.ifBlank { "#25D366" }

        val js = """
            (function() {
                try {
                    var existing = document.getElementById('wault-mobile-shell');
                    if (existing) existing.remove();

                    var style = document.createElement('style');
                    style.id = 'wault-mobile-shell';
                    style.textContent = `

                        /* ========== THEME VARIABLES ========== */
                        :root {
                            --wault-bg: #0b141a;
                            --wault-surface: #111b21;
                            --wault-surface-2: #16232b;
                            --wault-surface-3: #1f2c34;
                            --wault-text: #e9edef;
                            --wault-text-secondary: #aebac1;
                            --wault-accent: ${safeAccent};
                        }


                        /* ========== BASE RESET ========== */
                        html, body {
                            background: var(--wault-bg) !important;
                            color: var(--wault-text) !important;
                            overscroll-behavior: none !important;
                            overscroll-behavior-y: none !important;
                            overscroll-behavior-x: none !important;
                            overflow-x: hidden !important;
                            -webkit-tap-highlight-color: transparent !important;
                            -webkit-overflow-scrolling: touch !important;
                        }

                        body, div, span, button, a, input, textarea {
                            -webkit-tap-highlight-color: transparent !important;
                        }

                        #app {
                            background: var(--wault-bg) !important;
                            width: 100% !important;
                            max-width: 100vw !important;
                            overflow-x: hidden !important;
                        }


                        /* ========== SCROLLBAR ========== */
                        * {
                            scrollbar-width: thin !important;
                            scrollbar-color: rgba(255,255,255,0.08) transparent !important;
                        }
                        *::-webkit-scrollbar {
                            width: 3px !important;
                            height: 0px !important;
                        }
                        *::-webkit-scrollbar-track {
                            background: transparent !important;
                        }
                        *::-webkit-scrollbar-thumb {
                            background: rgba(255,255,255,0.08) !important;
                            border-radius: 999px !important;
                        }


                        /* ========== USER SELECTION ========== */
                        body:not([contenteditable="true"]),
                        body *:not(input):not(textarea):not([contenteditable="true"]) {
                            -webkit-user-select: none !important;
                            user-select: none !important;
                        }
                        input, textarea, [contenteditable="true"] {
                            -webkit-user-select: text !important;
                            user-select: text !important;
                        }


                        /* ========== WAULT ACCENT LINE ========== */
                        #app::before {
                            content: "";
                            position: fixed;
                            top: 0;
                            left: 0;
                            right: 0;
                            height: 2px;
                            background: linear-gradient(90deg, transparent 0%, var(--wault-accent) 50%, transparent 100%);
                            z-index: 2147483646;
                            pointer-events: none;
                            opacity: 0.6;
                        }


                        /* ========== PANEL SYSTEM ========== */

                        /* Both panels always full width */
                        #pane-side {
                            width: 100% !important;
                            min-width: 100% !important;
                            max-width: 100% !important;
                            flex: 1 1 100% !important;
                        }

                        #main {
                            width: 100% !important;
                            min-width: 100% !important;
                            max-width: 100% !important;
                            flex: 1 1 100% !important;
                        }

                        #side {
                            width: 100% !important;
                            min-width: 100% !important;
                            max-width: 100% !important;
                        }

                        /* DEFAULT: Chat list visible, conversation hidden */
                        body:not(.wault-chat-open) #main {
                            display: none !important;
                        }

                        /* CHAT OPEN: Conversation visible, chat list hidden */
                        body.wault-chat-open #pane-side {
                            display: none !important;
                        }

                        body.wault-chat-open #main {
                            display: flex !important;
                            flex-direction: column !important;
                        }


                        /* ========== CHAT LIST ========== */
                        [data-testid="chat-list"] {
                            background: var(--wault-bg) !important;
                        }

                        [data-testid="cell-frame-container"] {
                            border-radius: 12px !important;
                            margin: 1px 4px !important;
                        }

                        [data-testid="cell-frame-container"]:active {
                            background: rgba(255,255,255,0.04) !important;
                        }


                        /* ========== HEADERS ========== */
                        [data-testid="chat-list-header"],
                        [data-testid="chat-header"],
                        header {
                            background: var(--wault-surface) !important;
                            border-bottom: 1px solid rgba(255,255,255,0.04) !important;
                            box-shadow: none !important;
                            backdrop-filter: none !important;
                        }


                        /* ========== CONVERSATION ========== */
                        [data-testid="conversation-panel-wrapper"],
                        #main {
                            background: var(--wault-bg) !important;
                        }


                        /* ========== FOOTER / COMPOSE ========== */
                        footer {
                            background: var(--wault-surface) !important;
                            border-top: 1px solid rgba(255,255,255,0.04) !important;
                            width: 100% !important;
                        }

                        [data-testid="conversation-compose-box-input"] {
                            background: transparent !important;
                            color: var(--wault-text) !important;
                        }

                        [data-testid="conversation-compose-box-input"]::placeholder {
                            color: var(--wault-text-secondary) !important;
                        }


                        /* ========== MESSAGE BUBBLES ========== */
                        [data-testid="msg-container"] .message-in,
                        [data-testid="msg-container"] .message-out {
                            border-radius: 14px !important;
                            box-shadow: none !important;
                            max-width: 88% !important;
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
                            color: rgba(255,255,255,0.55) !important;
                        }


                        /* ========== SEARCH ========== */
                        [data-testid="chat-list-search"],
                        [data-testid="search"] {
                            background: rgba(255,255,255,0.06) !important;
                            border-radius: 20px !important;
                            border: 1px solid rgba(255,255,255,0.04) !important;
                            box-shadow: none !important;
                        }


                        /* ========== UNREAD BADGES ========== */
                        [data-testid="unread-count"],
                        [data-testid="icon-unread-count"] {
                            border-radius: 999px !important;
                            min-width: 20px !important;
                            font-weight: 600 !important;
                        }


                        /* ========== TOUCH-FRIENDLY ACTION BUTTONS ========== */
                        [data-testid="send-btn"],
                        [data-testid="audio-record-btn"],
                        [data-testid="attach-btn"],
                        [data-testid="emoji-btn"] {
                            min-width: 44px !important;
                            min-height: 44px !important;
                            display: flex !important;
                            align-items: center !important;
                            justify-content: center !important;
                        }

                        [data-testid="call-main-btn"],
                        [data-testid="call-video-btn"],
                        [data-testid="search-toolbar-button"],
                        [data-testid="chat-settings-button"] {
                            border-radius: 10px !important;
                            min-width: 40px !important;
                            min-height: 40px !important;
                        }


                        /* ========== MENU BAR (preserve Communities/Status/Channels) ========== */
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
                            opacity: 0.92 !important;
                        }


                        /* ========== QR / STARTUP SCREENS ========== */
                        #startup,
                        #landing {
                            background: var(--wault-bg) !important;
                            width: 100% !important;
                        }

                        body.wault-qr-mode,
                        body.wault-qr-mode #app,
                        body.wault-qr-mode #startup,
                        body.wault-qr-mode #landing {
                            background: var(--wault-bg) !important;
                        }

                        body.wault-qr-mode [data-testid="qrcode"] {
                            max-width: 264px !important;
                            margin: 0 auto !important;
                        }

                        body.wault-qr-mode canvas {
                            max-width: 100% !important;
                            height: auto !important;
                            border-radius: 16px !important;
                        }


                        /* ========== DIVIDERS / NOTIFICATIONS ========== */
                        [data-testid="notification-box"],
                        [data-testid="msg-date-divider"],
                        [data-testid="unread-messages-divider"] {
                            backdrop-filter: none !important;
                            border-radius: 12px !important;
                        }


                        /* ========== HIDE DESKTOP ARTIFACTS ========== */
                        [data-testid="download-mobile-app-banner"],
                        [data-testid="wa-web-banner"],
                        [data-testid="alert-phone"],
                        [data-testid="alert-computer"] {
                            display: none !important;
                        }


                        /* ========== POPUPS / CONTEXT MENUS ========== */
                        [data-testid="popup-contents"],
                        [role="application"] [data-testid="popup-contents"] {
                            background: var(--wault-surface-2) !important;
                            border-radius: 14px !important;
                            border: 1px solid rgba(255,255,255,0.06) !important;
                            box-shadow: 0 8px 32px rgba(0,0,0,0.45) !important;
                        }


                        /* ========== EMOJI / STICKER / GIF PANELS ========== */
                        [data-testid="emoji-panel"],
                        [data-testid="sticker-panel"],
                        [data-testid="gif-panel"] {
                            background: var(--wault-surface) !important;
                            border-top: 1px solid rgba(255,255,255,0.04) !important;
                            width: 100% !important;
                        }


                        /* ========== MEDIA ========== */
                        [data-testid="media-viewer"],
                        [data-testid="image-thumb"],
                        [data-testid="video-player"] {
                            max-width: 100% !important;
                            border-radius: 10px !important;
                        }


                        /* ========== QUOTED / LINK PREVIEW ========== */
                        [data-testid="quoted-message"] {
                            border-radius: 10px !important;
                            border-left: 3px solid var(--wault-accent) !important;
                        }

                        [data-testid="link-preview"] {
                            border-radius: 12px !important;
                            overflow: hidden !important;
                        }

                    `;
                    document.head.appendChild(style);
                    document.body.classList.add('wault-session-active');
                } catch (e) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    private fun injectWaultPanelController(webView: WebView) {
        val js = """
            (function() {
                try {
                    if (window.__waultPanelControllerInstalled) return;
                    window.__waultPanelControllerInstalled = true;

                    var previousChatOpen = false;

                    function hasActiveConversation() {
                        var main = document.getElementById('main');
                        if (!main) return false;
                        if (main.querySelector('footer')) return true;
                        if (main.querySelector('[data-testid="conversation-compose-box-input"]')) return true;
                        if (main.querySelector('[data-testid="conversation-panel-wrapper"]')) return true;
                        return false;
                    }

                    function updateState() {
                        var body = document.body;
                        if (!body) return;

                        var qr = document.querySelector('[data-testid="qrcode"]');

                        body.classList.remove('wault-qr-mode');

                        if (qr) {
                            body.classList.add('wault-qr-mode');
                            body.classList.remove('wault-chat-open');
                            previousChatOpen = false;
                            return;
                        }

                        var chatOpen = hasActiveConversation();

                        if (chatOpen !== previousChatOpen) {
                            previousChatOpen = chatOpen;

                            if (chatOpen) {
                                body.classList.add('wault-chat-open');
                                setTimeout(function() {
                                    var el = document.activeElement;
                                    if (el && el !== document.body && el.tagName !== 'BODY') {
                                        el.blur();
                                    }
                                }, 120);
                            } else {
                                body.classList.remove('wault-chat-open');
                            }
                        }
                    }

                    window.__waultHandleBack = function() {
                        if (document.body && document.body.classList.contains('wault-chat-open')) {
                            document.body.classList.remove('wault-chat-open');
                            previousChatOpen = false;
                            setTimeout(function() {
                                var el = document.activeElement;
                                if (el && el !== document.body) el.blur();
                            }, 50);
                            return 'true';
                        }
                        return 'false';
                    };

                    updateState();

                    var debounceTimer = null;
                    var observer = new MutationObserver(function() {
                        if (debounceTimer) return;
                        debounceTimer = setTimeout(function() {
                            debounceTimer = null;
                            updateState();
                        }, 80);
                    });

                    var target = document.body || document.documentElement;
                    if (target) {
                        observer.observe(target, {
                            childList: true,
                            subtree: true
                        });
                    }

                    setInterval(updateState, 600);

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

                    document.addEventListener('touchstart', function(e) {
                        if (e.touches && e.touches.length > 1) {
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

                    function detectLoginState() {
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
                    detectLoginState();
                    setInterval(sendUnreadCount, 3000);
                    setInterval(detectLoginState, 2000);

                } catch (e) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }
}
// File: android/app/src/main/kotlin/com/diva/wault/MainActivity.kt
package com.diva.wault

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private var eventSink: EventChannel.EventSink? = null
    private var eventReceiver: BroadcastReceiver? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            WaultChannels.ENGINE
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                WaultMethods.OPEN_SESSION -> handleOpenSession(call.arguments, result)
                WaultMethods.CLOSE_SESSION -> handleCloseSession(call.arguments, result)
                WaultMethods.CLOSE_ALL_SESSIONS -> handleCloseAllSessions(result)
                WaultMethods.GET_DEVICE_INFO -> handleGetDeviceInfo(result)
                WaultMethods.CAPTURE_SNAPSHOT -> handleCaptureSnapshot(call.arguments, result)
                else -> result.notImplemented()
            }
        }

        EventChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            WaultChannels.EVENTS
        ).setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                eventSink = events
                registerEventReceiver()
            }

            override fun onCancel(arguments: Any?) {
                unregisterEventReceiver()
                eventSink = null
            }
        })
    }

    private fun handleOpenSession(arguments: Any?, result: MethodChannel.Result) {
        val args = arguments as? Map<*, *>
        if (args == null) {
            result.error("INVALID_ARGS", "Arguments must be a map", null)
            return
        }

        val accountId = args["accountId"] as? String
        val label = args["label"] as? String
        val accentColor = args["accentColor"] as? String
        val processSlot = when (val slot = args["processSlot"]) {
            is Int -> slot
            is Number -> slot.toInt()
            else -> null
        }

        if (accountId == null || label == null || accentColor == null || processSlot == null) {
            result.error("MISSING_ARGS", "Missing required arguments", null)
            return
        }

        val activityClass = when (processSlot) {
            0 -> WebViewSessionActivity0::class.java
            1 -> WebViewSessionActivity1::class.java
            2 -> WebViewSessionActivity2::class.java
            3 -> WebViewSessionActivity3::class.java
            4 -> WebViewSessionActivity4::class.java
            else -> {
                result.error("INVALID_SLOT", "Process slot must be between 0 and 4", null)
                return
            }
        }

        val intent = Intent(this, activityClass).apply {
            putExtra(BaseWebViewSessionActivity.EXTRA_ACCOUNT_ID, accountId)
            putExtra(BaseWebViewSessionActivity.EXTRA_LABEL, label)
            putExtra(BaseWebViewSessionActivity.EXTRA_ACCENT_COLOR, accentColor)
            putExtra(BaseWebViewSessionActivity.EXTRA_PROCESS_SLOT, processSlot)
        }

        try {
            startActivity(intent)
            result.success(null)
        } catch (e: Exception) {
            result.error("LAUNCH_FAILED", e.message ?: "Unknown launch failure", null)
        }
    }

    private fun handleCloseSession(arguments: Any?, result: MethodChannel.Result) {
        val args = arguments as? Map<*, *>
        val accountId = args?.get("accountId") as? String

        if (accountId.isNullOrBlank()) {
            result.error("MISSING_ARGS", "accountId is required", null)
            return
        }

        EventBroadcaster.sendControlCloseSession(
            context = applicationContext,
            accountId = accountId
        )

        result.success(null)
    }

    private fun handleCloseAllSessions(result: MethodChannel.Result) {
        EventBroadcaster.sendControlCloseAll(applicationContext)
        result.success(null)
    }

    private fun handleGetDeviceInfo(result: MethodChannel.Result) {
        val info = DeviceProfiler.profile(this)
        result.success(
            mapOf(
                "totalRamMB" to info.totalRamMB,
                "availableRamMB" to info.availableRamMB,
                "cpuCores" to info.cpuCores,
                "tier" to info.tier.name,
                "maxAccounts" to info.maxAccounts,
                "maxWarm" to info.maxWarm
            )
        )
    }

    private fun handleCaptureSnapshot(arguments: Any?, result: MethodChannel.Result) {
        val args = arguments as? Map<*, *>
        val accountId = args?.get("accountId") as? String

        if (accountId.isNullOrBlank()) {
            result.error("MISSING_ARGS", "accountId is required", null)
            return
        }

        result.success(null)
    }

    private fun registerEventReceiver() {
        if (eventReceiver != null) return

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                if (intent.action != EventBroadcaster.ACTION) return

                val type = intent.getStringExtra(EventBroadcaster.KEY_TYPE) ?: return

                if (type == EventBroadcaster.TYPE_CONTROL_CLOSE_SESSION ||
                    type == EventBroadcaster.TYPE_CONTROL_CLOSE_ALL
                ) {
                    return
                }

                val event = mutableMapOf<String, Any>(
                    "type" to type
                )

                intent.getStringExtra(EventBroadcaster.KEY_ACCOUNT_ID)?.let {
                    event["accountId"] = it
                }

                if (intent.hasExtra(EventBroadcaster.KEY_COUNT)) {
                    event["count"] = intent.getIntExtra(EventBroadcaster.KEY_COUNT, 0)
                }

                if (intent.hasExtra(EventBroadcaster.KEY_STATE)) {
                    event["state"] = intent.getStringExtra(EventBroadcaster.KEY_STATE) ?: ""
                }

                if (intent.hasExtra(EventBroadcaster.KEY_MESSAGE)) {
                    event["message"] = intent.getStringExtra(EventBroadcaster.KEY_MESSAGE) ?: ""
                }

                if (intent.hasExtra(EventBroadcaster.KEY_DID_CRASH)) {
                    event["didCrash"] = intent.getBooleanExtra(EventBroadcaster.KEY_DID_CRASH, false)
                }

                runOnUiThread {
                    eventSink?.success(event)
                }
            }
        }

        val filter = IntentFilter(EventBroadcaster.ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }

        eventReceiver = receiver
    }

    private fun unregisterEventReceiver() {
        eventReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {
            }
        }
        eventReceiver = null
    }

    override fun onDestroy() {
        unregisterEventReceiver()
        super.onDestroy()
    }
}

private object WaultChannels {
    const val ENGINE = "com.diva.wault/engine"
    const val EVENTS = "com.diva.wault/events"
}

private object WaultMethods {
    const val OPEN_SESSION = "openSession"
    const val CLOSE_SESSION = "closeSession"
    const val CLOSE_ALL_SESSIONS = "closeAllSessions"
    const val GET_DEVICE_INFO = "getDeviceInfo"
    const val CAPTURE_SNAPSHOT = "captureSnapshot"
}
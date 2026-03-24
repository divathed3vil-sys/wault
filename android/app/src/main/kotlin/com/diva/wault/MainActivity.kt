// android/app/src/main/kotlin/com/diva/wault/MainActivity.kt

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
            "com.diva.wault/engine"
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "openSession" -> handleOpenSession(call.arguments, result)
                "closeSession" -> handleCloseSession(call.arguments, result)
                "closeAllSessions" -> handleCloseAllSessions(result)
                "getDeviceInfo" -> handleGetDeviceInfo(result)
                "captureSnapshot" -> handleCaptureSnapshot(call.arguments, result)
                else -> result.notImplemented()
            }
        }

        EventChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            "com.diva.wault/events"
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
        val processSlot = args["processSlot"] as? Int

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
            result.success(true)
        } catch (e: Exception) {
            result.error("LAUNCH_FAILED", e.message ?: "Unknown launch failure", null)
        }
    }

    private fun handleCloseSession(arguments: Any?, result: MethodChannel.Result) {
        val args = arguments as? Map<*, *>
        val accountId = args?.get("accountId") as? String
        if (accountId == null) {
            result.error("MISSING_ARGS", "accountId is required", null)
            return
        }

        sendBroadcast(
            Intent(EventBroadcaster.ACTION).apply {
                putExtra(EventBroadcaster.KEY_TYPE, EventBroadcaster.TYPE_CONTROL_CLOSE_SESSION)
                putExtra(EventBroadcaster.KEY_ACCOUNT_ID, accountId)
            }
        )

        result.success(true)
    }

    private fun handleCloseAllSessions(result: MethodChannel.Result) {
        sendBroadcast(
            Intent(EventBroadcaster.ACTION).apply {
                putExtra(EventBroadcaster.KEY_TYPE, EventBroadcaster.TYPE_CONTROL_CLOSE_ALL)
            }
        )
        result.success(true)
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
        if (accountId == null) {
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
                val accountId = intent.getStringExtra(EventBroadcaster.KEY_ACCOUNT_ID) ?: ""

                if (type == EventBroadcaster.TYPE_CONTROL_CLOSE_SESSION ||
                    type == EventBroadcaster.TYPE_CONTROL_CLOSE_ALL
                ) {
                    return
                }

                val event = mutableMapOf<String, Any>(
                    "type" to type,
                    "accountId" to accountId
                )

                if (intent.hasExtra(EventBroadcaster.KEY_COUNT)) {
                    event["count"] = intent.getIntExtra(EventBroadcaster.KEY_COUNT, 0)
                }
                if (intent.hasExtra(EventBroadcaster.KEY_STATE)) {
                    event["state"] = intent.getStringExtra(EventBroadcaster.KEY_STATE) ?: ""
                }
                if (intent.hasExtra(EventBroadcaster.KEY_MESSAGE)) {
                    event["message"] = intent.getStringExtra(EventBroadcaster.KEY_MESSAGE) ?: ""
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
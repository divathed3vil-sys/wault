// android/app/src/main/kotlin/com/diva/wault/EventBroadcaster.kt

package com.diva.wault

import android.content.Context
import android.content.Intent

object EventBroadcaster {

    const val ACTION = "com.diva.wault.EVENT"

    const val KEY_TYPE = "type"
    const val KEY_ACCOUNT_ID = "accountId"
    const val KEY_COUNT = "count"
    const val KEY_STATE = "state"
    const val KEY_MESSAGE = "message"

    const val TYPE_UNREAD_COUNT = "unreadCount"
    const val TYPE_QR_VISIBLE = "qrVisible"
    const val TYPE_LOGGED_IN = "loggedIn"
    const val TYPE_SESSION_CRASHED = "sessionCrashed"
    const val TYPE_SESSION_STATE_CHANGED = "sessionStateChanged"
    const val TYPE_SESSION_ERROR = "sessionError"

    const val TYPE_CONTROL_CLOSE_SESSION = "closeSession"
    const val TYPE_CONTROL_CLOSE_ALL = "closeAllSessions"

    fun sendUnreadCount(context: Context, accountId: String, count: Int) {
        send(
            context = context,
            type = TYPE_UNREAD_COUNT,
            accountId = accountId,
            count = count
        )
    }

    fun sendQrVisible(context: Context, accountId: String) {
        send(
            context = context,
            type = TYPE_QR_VISIBLE,
            accountId = accountId
        )
    }

    fun sendLoggedIn(context: Context, accountId: String) {
        send(
            context = context,
            type = TYPE_LOGGED_IN,
            accountId = accountId
        )
    }

    fun sendSessionCrashed(context: Context, accountId: String) {
        send(
            context = context,
            type = TYPE_SESSION_CRASHED,
            accountId = accountId
        )
    }

    fun sendSessionStateChanged(context: Context, accountId: String, state: String) {
        send(
            context = context,
            type = TYPE_SESSION_STATE_CHANGED,
            accountId = accountId,
            state = state
        )
    }

    fun sendSessionError(context: Context, accountId: String, message: String) {
        send(
            context = context,
            type = TYPE_SESSION_ERROR,
            accountId = accountId,
            message = message
        )
    }

    private fun send(
        context: Context,
        type: String,
        accountId: String = "",
        count: Int? = null,
        state: String? = null,
        message: String? = null
    ) {
        val intent = Intent(ACTION).apply {
            putExtra(KEY_TYPE, type)
            putExtra(KEY_ACCOUNT_ID, accountId)
            if (count != null) putExtra(KEY_COUNT, count)
            if (state != null) putExtra(KEY_STATE, state)
            if (message != null) putExtra(KEY_MESSAGE, message)
        }
        context.sendBroadcast(intent)
    }
}
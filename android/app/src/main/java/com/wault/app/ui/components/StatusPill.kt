package com.wault.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wault.app.R
import com.wault.app.data.AccountStatus
import com.wault.app.ui.theme.StatusError
import com.wault.app.ui.theme.StatusMuted
import com.wault.app.ui.theme.StatusOk
import com.wault.app.ui.theme.StatusPending

/**
 * Rounded status pill with a colored dot: Connected emerald, Connecting/Qr/
 * Reconnecting amber, Disconnected gray, Error rose. Same mapping as the web.
 */
@Composable
fun StatusPill(status: AccountStatus, modifier: Modifier = Modifier) {
    val dotColor = when (status) {
        AccountStatus.Connected -> StatusOk
        AccountStatus.Connecting, AccountStatus.Qr, AccountStatus.Reconnecting -> StatusPending
        AccountStatus.Disconnected -> StatusMuted
        AccountStatus.Error -> StatusError
    }
    val label = stringResource(
        when (status) {
            AccountStatus.Connected -> R.string.status_connected
            AccountStatus.Connecting -> R.string.status_connecting
            AccountStatus.Qr -> R.string.status_qr
            AccountStatus.Reconnecting -> R.string.status_reconnecting
            AccountStatus.Disconnected -> R.string.status_disconnected
            AccountStatus.Error -> R.string.status_error
        },
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = dotColor.copy(alpha = 0.14f),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(dotColor, CircleShape),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = dotColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

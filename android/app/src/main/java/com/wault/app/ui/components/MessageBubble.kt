package com.wault.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DoneAll
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wault.app.R
import com.wault.app.data.Message
import com.wault.app.data.MessageKind
import com.wault.app.domain.engine.Receipt
import com.wault.app.util.Formatters

/**
 * Wault chat bubble, mirroring the web demo: outgoing bubbles are tonal in
 * the account accent (translucent accent container), incoming bubbles are
 * plain surface. Outgoing rows carry receipt ticks: single check = Sent,
 * double check = Delivered, double check in the account accent = Read.
 */
@Composable
fun MessageBubble(
    message: Message,
    isGroupChat: Boolean,
    accountAccent: Color,
    modifier: Modifier = Modifier,
) {
    val isOut = message.isFromMe
    val cornerRadius = 20.dp
    val shape = RoundedCornerShape(
        topStart = cornerRadius,
        topEnd = cornerRadius,
        bottomEnd = if (isOut) 6.dp else cornerRadius,
        bottomStart = if (isOut) cornerRadius else 6.dp,
    )
    Column(
        horizontalAlignment = if (isOut) Alignment.End else Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
    ) {
        if (!isOut && isGroupChat) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelMedium,
                color = accountAccent,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
            )
        }
        Surface(
            shape = shape,
            color = if (isOut) accountAccent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
            border = if (isOut) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                if (message.kind == MessageKind.Media) {
                    MediaPlaceholder(isOut = isOut, accent = accountAccent)
                    Spacer(modifier = Modifier.size(6.dp))
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text = Formatters.messageTime(message.timestampEpochMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isOut) {
                        Spacer(modifier = Modifier.size(4.dp))
                        ReceiptTicks(receipt = message.receipt, accent = accountAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaPlaceholder(isOut: Boolean, accent: Color) {
    val tint = if (isOut) accent else MaterialTheme.colorScheme.primary
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 208.dp, height = 128.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = if (isOut) 0.16f else 0.12f)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = stringResource(R.string.media_placeholder),
                tint = tint,
                modifier = Modifier.size(30.dp),
            )
            Text(
                text = stringResource(R.string.media_placeholder),
                style = MaterialTheme.typography.labelSmall,
                color = tint,
            )
        }
    }
}

@Composable
private fun ReceiptTicks(receipt: Receipt?, accent: Color) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    when (receipt) {
        null -> Unit
        Receipt.Sent -> TickIcon(Icons.Outlined.Check, muted)
        Receipt.Delivered -> TickIcon(Icons.AutoMirrored.Outlined.DoneAll, muted)
        Receipt.Read -> TickIcon(Icons.AutoMirrored.Outlined.DoneAll, accent)
    }
}

@Composable
private fun TickIcon(imageVector: ImageVector, tint: Color) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(14.dp),
    )
}

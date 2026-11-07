package com.wault.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wault.app.ui.theme.parseHexAccent
import com.wault.app.util.Formatters

/**
 * Accent-tinted initials avatar — the identity glyph used on account cards,
 * chat rows and conversation headers. Tint comes from the owning account's
 * accent hex, exactly like the web demo.
 */
@Composable
fun AccountAvatar(
    name: String,
    accentHex: String,
    size: Dp,
    modifier: Modifier = Modifier,
    showRing: Boolean = false,
) {
    val accent = parseHexAccent(accentHex)
    val ringModifier = if (showRing) {
        Modifier.border(1.5.dp, accent.copy(alpha = 0.55f), CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(size)
            .background(accent.copy(alpha = 0.16f), CircleShape)
            .then(ringModifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = Formatters.initialsOf(name),
            color = accent,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = (size.value * 0.34f).sp,
                fontWeight = FontWeight.SemiBold,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

/** Small accent-filled unread counter; hidden at zero. */
@Composable
fun UnreadBadge(
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    Surface(
        shape = CircleShape,
        color = accent,
        modifier = modifier,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

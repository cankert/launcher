package de.dm.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.dm.launcher.notif.NotificationsState

@Composable
fun NotificationIndicator(modifier: Modifier = Modifier) {
    val hasActive by NotificationsState.hasActive.collectAsState()
    if (!hasActive) return
    Box(
        modifier = modifier
            .padding(start = 36.dp, top = 12.dp)
            .width(22.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(1.5.dp))
            .background(Color.White.copy(alpha = 0.8f))
    )
}

package de.dm.launcher.ui.home

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.dm.launcher.domain.LaunchAppUseCase

@Composable
fun HomeScreen(
    onSwipeDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { 64.dp.toPx() }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var triggered by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        dragAccum = 0f
                        triggered = 0f
                    },
                    onDragEnd = { dragAccum = 0f },
                    onDragCancel = { dragAccum = 0f }
                ) { _, dragAmount ->
                    dragAccum += dragAmount
                    if (dragAccum > thresholdPx && triggered == 0f) {
                        triggered = 1f
                        onSwipeDown()
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.22f))
            ClockWithBatteryRing(
                onClick = { LaunchAppUseCase.openAlarmClock(context) }
            )
            Spacer(Modifier.weight(0.10f))
            PinnedAppsList(
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.weight(0.40f))
        }
        SystemShortcutsBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
        NotificationIndicator(
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

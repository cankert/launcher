package de.dm.launcher.ui.drawer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.dm.launcher.domain.AppInfo
import kotlinx.coroutines.launch

private val LETTERS = ('A'..'Z').toList() + '#'

@Composable
fun AzScrollBar(
    apps: List<AppInfo>,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var heightPx by remember { mutableFloatStateOf(0f) }
    var lastLetter by remember { mutableFloatStateOf(-1f) }

    val letterToIndex = remember(apps) {
        val map = HashMap<Char, Int>()
        apps.forEachIndexed { idx, app ->
            val first = app.label.firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (first in 'A'..'Z') first else '#'
            map.putIfAbsent(key, idx)
        }
        map
    }

    fun jumpToY(y: Float) {
        if (heightPx <= 0f || LETTERS.isEmpty()) return
        val pos = (y / heightPx).coerceIn(0f, 0.9999f)
        val letterIdx = (pos * LETTERS.size).toInt().coerceIn(0, LETTERS.lastIndex)
        if (letterIdx.toFloat() == lastLetter) return
        lastLetter = letterIdx.toFloat()
        val letter = LETTERS[letterIdx]
        val targetIdx = letterToIndex[letter]
            ?: letterToIndex.entries.filter { it.key <= letter }.maxByOrNull { it.key }?.value
            ?: return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch { listState.scrollToItem(targetIdx) }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(36.dp) // breitere Touch-Fläche
            .padding(end = 4.dp)
            .onSizeChanged { heightPx = it.height.toFloat() }
            .pointerInput(apps) {
                detectTapGestures(
                    onPress = { offset -> jumpToY(offset.y) }
                )
            }
            .pointerInput(apps) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> jumpToY(offset.y) },
                    onDragEnd = { lastLetter = -1f },
                    onDragCancel = { lastLetter = -1f }
                ) { change, _ ->
                    jumpToY(change.position.y)
                }
            },
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = androidx.compose.ui.Alignment.End
    ) {
        LETTERS.forEach { letter ->
            Text(
                text = letter.toString(),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

package de.dm.launcher.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import de.dm.launcher.data.AppRepository
import de.dm.launcher.data.PreferencesRepository
import de.dm.launcher.data.formatUsage
import de.dm.launcher.data.rememberTodayUsageMinutes
import de.dm.launcher.domain.LaunchAppUseCase
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ITEM_SPACING = 18.dp

@Composable
fun PinnedAppsList(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val prefs = remember(context) { PreferencesRepository(context) }
    val repo = remember(context) { AppRepository(context) }
    val pinnedPersisted by prefs.pinnedPackages.collectAsState(initial = emptyList())
    val usageMillis by rememberTodayUsageMinutes(context)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val itemSpacingPx = with(density) { ITEM_SPACING.toPx() }

    var draggedPkg by remember { mutableStateOf<String?>(null) }
    var dragStartIdx by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }

    val items = remember(pinnedPersisted) {
        pinnedPersisted.mapNotNull { pkg ->
            val label = repo.getLabel(pkg) ?: return@mapNotNull null
            pkg to label
        }
    }

    val rowH = itemHeightPx + itemSpacingPx
    // Aktuelle Ziel-Position des gedragten Items (basierend auf Drag-Offset)
    val targetIdx = if (draggedPkg != null && rowH > 0f && dragStartIdx >= 0) {
        (dragStartIdx + (dragOffsetY / rowH).roundToInt())
            .coerceIn(0, items.lastIndex)
    } else dragStartIdx

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(ITEM_SPACING)
    ) {
        items.forEachIndexed { idx, (pkg, label) ->
            key(pkg) {
                val isDragged = pkg == draggedPkg

                // Andere Items rücken hoch/runter um Platz fürs gedragte Item zu machen
                val shiftTarget = when {
                    isDragged -> 0f // Dragged item nutzt eigene translation (dragOffsetY)
                    draggedPkg == null || dragStartIdx < 0 -> 0f
                    // Item liegt im Bereich [targetIdx, dragStartIdx-1] → muss nach unten rücken
                    idx in targetIdx until dragStartIdx -> rowH
                    // Item liegt im Bereich [dragStartIdx+1, targetIdx] → muss nach oben rücken
                    idx in (dragStartIdx + 1)..targetIdx -> -rowH
                    else -> 0f
                }
                val animatedShift by animateFloatAsState(
                    targetValue = shiftTarget,
                    animationSpec = tween(durationMillis = 180),
                    label = "shift-$pkg"
                )

                val usageText = formatUsage(usageMillis[pkg] ?: 0L)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            itemHeightPx = size.height.toFloat()
                        }
                        .graphicsLayer {
                            translationY = if (isDragged) dragOffsetY else animatedShift
                        }
                        .zIndex(if (isDragged) 1f else 0f)
                        .pointerInput(pkg) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggedPkg = pkg
                                    // Index frisch aus dem aktuell persistierten State holen
                                    dragStartIdx = pinnedPersisted.indexOf(pkg)
                                    dragOffsetY = 0f
                                },
                                onDragEnd = {
                                    // Final-Target frisch berechnen aus aktuellen State-Werten
                                    val startIdx = dragStartIdx
                                    val offset = dragOffsetY
                                    val height = itemHeightPx
                                    val list = pinnedPersisted
                                    val rowHeight = height + itemSpacingPx
                                    if (startIdx in list.indices && rowHeight > 0f) {
                                        val finalTarget = (startIdx +
                                                (offset / rowHeight).roundToInt())
                                            .coerceIn(0, list.lastIndex)
                                        if (finalTarget != startIdx) {
                                            val newOrder = list.toMutableList().apply {
                                                add(finalTarget, removeAt(startIdx))
                                            }
                                            scope.launch { prefs.setPinned(newOrder) }
                                        }
                                    }
                                    draggedPkg = null
                                    dragStartIdx = -1
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    draggedPkg = null
                                    dragStartIdx = -1
                                    dragOffsetY = 0f
                                }
                            ) { change, dragAmount ->
                                dragOffsetY += dragAmount.y
                                change.consume()
                            }
                        }
                        .clickable(enabled = !isDragged) {
                            LaunchAppUseCase.launchApp(context, pkg)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = if (isDragged) Color.White.copy(alpha = 0.55f) else Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal
                    )
                    if (usageText.isNotEmpty()) {
                        Text(
                            text = usageText,
                            color = Color.White.copy(alpha = if (isDragged) 0.25f else 0.5f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 10.dp, top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

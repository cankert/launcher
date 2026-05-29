package de.dm.launcher.ui.home

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.dm.launcher.data.BatteryProvider
import de.dm.launcher.data.BatteryState
import java.util.Calendar
import java.util.Locale

@Composable
fun ClockWithBatteryRing(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val context = LocalContext.current
    val time = rememberTimeTick(context)
    val batteryFlow = remember(context) { BatteryProvider.stateFlow(context) }
    val battery by batteryFlow.collectAsState(initial = BatteryState(0, false))
    val nextAlarm = rememberNextAlarm(context, time)

    val timeFmt = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    val locale = Locale.getDefault()
    val timeStr = remember(time, timeFmt) {
        DateFormat.format(timeFmt, time).toString()
    }
    val dateStr = remember(time, locale) {
        val cal = Calendar.getInstance(locale).apply { timeInMillis = time }
        val weekday = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, locale).orEmpty()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, locale).orEmpty()
        "$weekday, $day $month"
    }

    val pct = battery.percent.coerceIn(0, 100)
    val ringColor = when {
        pct < 15 -> Color(0xFFFF5252)
        pct < 30 -> Color(0xFFFFB74D)
        else -> Color.White
    }
    val density = LocalDensity.current
    val strokePx = with(density) { 2.dp.toPx() }

    // Ladeanimation: sanftes Pulsieren zwischen normaler Ringfarbe und Grün
    val chargingTransition = rememberInfiniteTransition(label = "charging")
    val pulse by chargingTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chargingPulse"
    )
    val chargingColor = Color(0xFF4CAF50) // material green 500

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val pad = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(pad, pad)
            // Hintergrund-Ring (dezent)
            drawArc(
                color = Color(0xFF1F1F1F),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )
            val activeColor = if (battery.isCharging) {
                androidx.compose.ui.graphics.lerp(ringColor, chargingColor, pulse)
            } else {
                ringColor
            }
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = (pct / 100f) * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = timeStr,
                color = Color.White,
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = dateStr,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp
            )
            if (nextAlarm != null) {
                Text(
                    text = "⏰ $nextAlarm",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun rememberNextAlarm(context: Context, timeTick: Long): String? {
    val alarmManager = remember(context) {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
    return remember(timeTick) {
        val info = alarmManager.nextAlarmClock ?: return@remember null
        val triggerMs = info.triggerTime
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = triggerMs }
        val deltaDays =
            ((target.timeInMillis - now.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
        val locale = Locale.getDefault()
        val timeFmt = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        val timePart = DateFormat.format(timeFmt, triggerMs).toString()
        val sameDay = target.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        when {
            sameDay -> timePart
            deltaDays < 6 -> {
                val weekday = target.getDisplayName(
                    Calendar.DAY_OF_WEEK, Calendar.SHORT, locale
                ).orEmpty()
                "$weekday $timePart"
            }
            else -> {
                val day = target.get(Calendar.DAY_OF_MONTH)
                val month = target.getDisplayName(
                    Calendar.MONTH, Calendar.SHORT, locale
                ).orEmpty()
                "$day $month $timePart"
            }
        }
    }
}

@Composable
private fun rememberTimeTick(context: Context): Long {
    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    DisposableEffect(context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                time = System.currentTimeMillis()
            }
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return time
}

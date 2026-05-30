package de.dm.launcher.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Process
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

object UsageStatsAccess {

    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/**
 * Liefert pro Package die Vordergrund-Nutzung des heutigen Tages (in Millisekunden).
 * Aktualisiert sich minütlich. Leere Map, wenn keine Permission.
 */
@Composable
fun rememberTodayUsageMinutes(context: Context): State<Map<String, Long>> {
    var trigger by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val filter = IntentFilter(Intent.ACTION_TIME_TICK)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { trigger++ }
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    return produceState(initialValue = emptyMap(), trigger) {
        value = withContext(Dispatchers.IO) { loadStatsForToday(context) }
    }
}

private fun loadStatsForToday(context: Context): Map<String, Long> {
    if (!UsageStatsAccess.isGranted(context)) return emptyMap()
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        ?: return emptyMap()

    val midnight = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val now = System.currentTimeMillis()

    // queryEvents liefert die rohen Foreground/Background-Events in Echtzeit (im Gegensatz
    // zu queryUsageStats, dessen Tages-Bucket erst alle paar Minuten aktualisiert wird).
    val events = usm.queryEvents(midnight, now) ?: return emptyMap()
    val totals = HashMap<String, Long>()
    val foregroundSince = HashMap<String, Long>() // pkg → timestamp wann Foreground begann
    val event = UsageEvents.Event()

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val pkg = event.packageName ?: continue
        when (event.eventType) {
            UsageEvents.Event.MOVE_TO_FOREGROUND,
            UsageEvents.Event.ACTIVITY_RESUMED -> {
                foregroundSince[pkg] = event.timeStamp
            }
            UsageEvents.Event.MOVE_TO_BACKGROUND,
            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.ACTIVITY_STOPPED -> {
                val start = foregroundSince.remove(pkg) ?: continue
                val duration = (event.timeStamp - start).coerceAtLeast(0)
                totals.merge(pkg, duration) { a, b -> a + b }
            }
        }
    }
    // Apps die JETZT noch im Vordergrund sind: laufenden Session-Anteil dazurechnen
    for ((pkg, start) in foregroundSince) {
        val duration = (now - start).coerceAtLeast(0)
        totals.merge(pkg, duration) { a, b -> a + b }
    }
    return totals
}

fun formatUsage(millis: Long): String {
    val totalMinutes = (millis / 60_000L).toInt()
    return when {
        totalMinutes < 1 -> ""
        totalMinutes < 60 -> "${totalMinutes}m"
        else -> {
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            if (m == 0) "${h}h" else "${h}h ${m}m"
        }
    }
}

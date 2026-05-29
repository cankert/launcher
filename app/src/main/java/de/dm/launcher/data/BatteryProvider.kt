package de.dm.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class BatteryState(val percent: Int, val isCharging: Boolean)

object BatteryProvider {

    fun stateFlow(context: Context): Flow<BatteryState> = callbackFlow {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return
                trySend(parse(intent))
            }
        }
        val sticky = context.registerReceiver(receiver, filter)
        if (sticky != null) trySend(parse(sticky))
        awaitClose { context.unregisterReceiver(receiver) }
    }

    private fun parse(intent: Intent): BatteryState {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        return BatteryState(percent.coerceIn(0, 100), isCharging)
    }
}

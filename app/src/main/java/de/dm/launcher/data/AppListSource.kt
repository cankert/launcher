package de.dm.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.dm.launcher.domain.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lädt die App-Liste einmalig beim ersten Compose und hält sie aktuell, indem PACKAGE_ADDED/
 * REMOVED/REPLACED-Broadcasts beobachtet werden. Beim ersten Render gibt es ein "loading"-
 * State, danach bleibt die Liste persistent im Memory, solange die App läuft.
 */
@Composable
fun rememberAppList(context: Context): State<List<AppInfo>> {
    val repo = remember(context) { AppRepository(context) }
    var trigger by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                trigger++
            }
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    return produceState(initialValue = emptyList(), trigger) {
        value = withContext(Dispatchers.IO) { repo.loadLaunchableApps() }
    }
}

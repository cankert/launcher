package de.dm.launcher.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import de.dm.launcher.data.PreferencesRepository
import de.dm.launcher.data.UsageStatsAccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun UsageAccessPrompt() {
    val context = LocalContext.current
    val prefs = remember(context) { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val asked = prefs.askedUsageAccess.first()
        if (!asked && !UsageStatsAccess.isGranted(context)) {
            visible = true
        }
    }

    if (!visible) return

    AlertDialog(
        onDismissRequest = {
            scope.launch { prefs.setAskedUsageAccess() }
            visible = false
        },
        title = { Text("Nutzungszeit anzeigen", color = Color.White) },
        text = {
            Text(
                "Damit neben jedem Favoriten die heutige Nutzungszeit dezent eingeblendet werden " +
                        "kann, braucht der Launcher Zugriff auf Nutzungsdaten.",
                color = Color.White.copy(alpha = 0.85f)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch { prefs.setAskedUsageAccess() }
                visible = false
                context.startActivity(UsageStatsAccess.settingsIntent())
            }) {
                Text("Einstellungen öffnen", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                scope.launch { prefs.setAskedUsageAccess() }
                visible = false
            }) {
                Text("Nicht jetzt", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = Color(0xFF101010)
    )
}

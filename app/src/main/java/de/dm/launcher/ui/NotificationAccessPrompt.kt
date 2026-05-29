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
import de.dm.launcher.notif.NotificationsState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun NotificationAccessPrompt() {
    val context = LocalContext.current
    val prefs = remember(context) { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val asked = prefs.askedNotifAccess.first()
        if (!asked && !NotificationsState.isAccessGranted(context)) {
            visible = true
        }
    }

    if (!visible) return

    AlertDialog(
        onDismissRequest = {
            scope.launch { prefs.setAskedNotifAccess() }
            visible = false
        },
        title = { Text("Benachrichtigungs-Indikator", color = Color.White) },
        text = {
            Text(
                "Damit ein dezenter Strich oben links erscheint, wenn aktive Benachrichtigungen vorliegen, " +
                        "muss der Launcher Zugriff auf Benachrichtigungen erhalten. Sonst bleibt die Funktion einfach aus.",
                color = Color.White.copy(alpha = 0.85f)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch { prefs.setAskedNotifAccess() }
                visible = false
                context.startActivity(NotificationsState.settingsIntent())
            }) {
                Text("Einstellungen öffnen", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                scope.launch { prefs.setAskedNotifAccess() }
                visible = false
            }) {
                Text("Nicht jetzt", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = Color(0xFF101010)
    )
}

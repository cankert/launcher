package de.dm.launcher.ui

import android.content.Context
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import de.dm.launcher.data.PreferencesRepository
import de.dm.launcher.domain.LaunchAppUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun DefaultLauncherPrompt() {
    val context = LocalContext.current
    val prefs = remember(context) { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val asked = prefs.askedDefaultLauncher.first()
        if (!asked && !isDefaultLauncher(context)) {
            visible = true
        }
    }

    if (!visible) return

    AlertDialog(
        onDismissRequest = {
            scope.launch { prefs.setAskedDefaultLauncher() }
            visible = false
        },
        title = { Text("Standard-Launcher festlegen", color = Color.White) },
        text = {
            Text(
                "Damit der Launcher beim Druck auf den Home-Button erscheint, muss er als Standard-Launcher festgelegt werden.",
                color = Color.White.copy(alpha = 0.85f)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch { prefs.setAskedDefaultLauncher() }
                visible = false
                LaunchAppUseCase.openHomeSettings(context)
            }) {
                Text("Einstellungen öffnen", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                scope.launch { prefs.setAskedDefaultLauncher() }
                visible = false
            }) {
                Text("Später", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = Color(0xFF101010)
    )
}

private fun isDefaultLauncher(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolveInfo = context.packageManager.resolveActivity(intent, 0) ?: return false
    return resolveInfo.activityInfo.packageName == context.packageName
}

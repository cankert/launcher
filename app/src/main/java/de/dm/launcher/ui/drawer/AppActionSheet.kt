package de.dm.launcher.ui.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AppActionTarget(
    val packageName: String,
    val label: String,
    val isPinned: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionSheet(
    target: AppActionTarget,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onUninstall: () -> Unit,
    onAppInfo: () -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Color(0xFF101010)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = target.label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            ActionRow(
                label = if (target.isPinned) "Aus Favoriten entfernen" else "Zu Favoriten hinzufügen",
                onClick = onTogglePin
            )
            ActionRow(label = "Deinstallieren", onClick = onUninstall)
            ActionRow(label = "App-Info", onClick = onAppInfo)
        }
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 17.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    )
}

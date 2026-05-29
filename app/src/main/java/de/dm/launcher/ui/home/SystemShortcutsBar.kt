package de.dm.launcher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.dm.launcher.domain.LaunchAppUseCase

@Composable
fun SystemShortcutsBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { LaunchAppUseCase.openDialer(context) }) {
            Icon(
                imageVector = Icons.Outlined.Phone,
                contentDescription = "Telefon",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        IconButton(onClick = { LaunchAppUseCase.openCamera(context) }) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = "Kamera",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

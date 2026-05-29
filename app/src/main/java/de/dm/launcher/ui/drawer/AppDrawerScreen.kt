package de.dm.launcher.ui.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.dm.launcher.data.AppRepository
import de.dm.launcher.data.PreferencesRepository
import de.dm.launcher.domain.AppInfo
import de.dm.launcher.domain.LaunchAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppDrawerScreen(
    apps: List<AppInfo>,
    isVisible: Boolean,
    onLongPressApp: (AppActionTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) { PreferencesRepository(context) }
    val pinned by prefs.pinnedPackages.collectAsState(initial = emptyList())
    val pinnedSet = remember(pinned) { pinned.toSet() }
    val allApps = apps

    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, allApps) {
        if (query.isBlank()) allApps
        else allApps.filter { it.label.contains(query, ignoreCase = true) }
    }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(query) {
        if (filtered.isNotEmpty()) listState.scrollToItem(0)
    }

    // Wenn der Drawer verlassen wird: Tastatur weg, Fokus weg, Suchfeld leeren
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            keyboard?.hide()
            focusManager.clearFocus(force = true)
            query = ""
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(48.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Box {
                    if (query.isEmpty()) {
                        Text(
                            "Apps suchen",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 20.sp
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 20.sp
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (filtered.isEmpty() && allApps.isNotEmpty()) {
                    Text(
                        text = "Keine Treffer",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 16.sp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 32.dp)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            AppListItem(
                                app = app,
                                onClick = { LaunchAppUseCase.launchApp(context, app.packageName) },
                                onLongPress = {
                                    onLongPressApp(
                                        AppActionTarget(
                                            packageName = app.packageName,
                                            label = app.label,
                                            isPinned = pinnedSet.contains(app.packageName)
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
                AzScrollBar(
                    apps = filtered,
                    listState = listState,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

package de.dm.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import de.dm.launcher.data.AppRepository
import de.dm.launcher.data.PreferencesRepository
import de.dm.launcher.data.rememberAppList
import de.dm.launcher.domain.LaunchAppUseCase
import de.dm.launcher.ui.drawer.AppActionSheet
import de.dm.launcher.ui.drawer.AppActionTarget
import de.dm.launcher.ui.drawer.AppDrawerScreen
import de.dm.launcher.ui.home.HomeScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherRoot() {
    val context = LocalContext.current
    val prefs = remember(context) { PreferencesRepository(context) }
    val repo = remember(context) { AppRepository(context) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val pinned by prefs.pinnedPackages.collectAsState(initial = emptyList())
    val apps by rememberAppList(context)
    var actionTarget by remember { mutableStateOf<AppActionTarget?>(null) }

    // Auto-prune uninstalled pinned apps on first composition only
    LaunchedEffect(Unit) {
        val current = prefs.pinnedPackages.first()
        val stillInstalled = current.filter { repo.isInstalled(it) }
        if (stillInstalled.size != current.size) {
            prefs.setPinned(stillInstalled)
        }
    }

    // Back-Geste aus dem Drawer → zurück zu Home. Auf Home ignoriert das System Back ohnehin
    // (Launcher-Activity), daher BackHandler nur aktiv wenn nicht auf Home.
    BackHandler(enabled = pagerState.currentPage != 0) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    // Home-Geste (Wisch nach oben vom unteren Rand) → auch zurück zu Home-Page animieren.
    // Wird gefeuert von MainActivity.onNewIntent.
    LaunchedEffect(Unit) {
        HomeGestureSignal.events.collect {
            if (pagerState.currentPage != 0) {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> HomeScreen(
                    onSwipeDown = { LaunchAppUseCase.expandNotifications(context) }
                )
                1 -> AppDrawerScreen(
                    apps = apps,
                    isVisible = pagerState.settledPage == 1 && pagerState.targetPage == 1,
                    onLongPressApp = { target ->
                        actionTarget = target.copy(isPinned = pinned.contains(target.packageName))
                    }
                )
            }
        }

        DefaultLauncherPrompt()
        NotificationAccessPrompt()

        actionTarget?.let { target ->
            AppActionSheet(
                target = target,
                onDismiss = { actionTarget = null },
                onTogglePin = {
                    scope.launch {
                        prefs.togglePin(target.packageName)
                        actionTarget = null
                    }
                },
                onUninstall = {
                    LaunchAppUseCase.requestUninstall(context, target.packageName)
                    actionTarget = null
                },
                onAppInfo = {
                    LaunchAppUseCase.openAppInfo(context, target.packageName)
                    actionTarget = null
                }
            )
        }
    }
}

package de.dm.launcher.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-weiter Signal-Flow: feuert jedes Mal, wenn der User die Home-Geste auslöst, während
 * unser Launcher bereits im Vordergrund ist. Wird von MainActivity.onNewIntent gespeist und
 * von LauncherRoot abonniert.
 */
object HomeGestureSignal {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun fire() {
        _events.tryEmit(Unit)
    }
}

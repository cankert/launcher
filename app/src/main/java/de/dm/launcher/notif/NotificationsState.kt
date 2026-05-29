package de.dm.launcher.notif

import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationsState {
    private val _hasActive = MutableStateFlow(false)
    val hasActive: StateFlow<Boolean> = _hasActive.asStateFlow()

    fun update(service: NotificationListenerService) {
        try {
            val all = service.activeNotifications ?: emptyArray()
            // Nur Notifications, die der User auch tatsächlich wegwischen kann
            // (filtert Foreground-Services, dauerhafte System-Notifs, FLAG_NO_CLEAR raus).
            // Group-Summaries ausklammern, damit eine WhatsApp-Konversation nicht doppelt zählt.
            val relevant = all.filter { sbn ->
                val n = sbn.notification ?: return@filter false
                val isGroupSummary = (n.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0
                sbn.isClearable && !isGroupSummary
            }
            _hasActive.value = relevant.isNotEmpty()
        } catch (_: Throwable) {
            // Service noch nicht ready
        }
    }

    fun isAccessGranted(context: Context): Boolean {
        val enabled = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabled.contains(context.packageName)
    }

    fun settingsIntent(): android.content.Intent =
        android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
}

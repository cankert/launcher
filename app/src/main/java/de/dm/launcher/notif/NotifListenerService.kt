package de.dm.launcher.notif

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationsState.update(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        NotificationsState.update(this)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        NotificationsState.update(this)
    }
}

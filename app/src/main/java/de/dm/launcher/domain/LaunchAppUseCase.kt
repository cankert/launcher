package de.dm.launcher.domain

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast

object LaunchAppUseCase {

    fun launchApp(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            toast(context, "App nicht gefunden")
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        safeStart(context, intent)
    }

    fun openAlarmClock(context: Context) {
        safeStart(context, Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openDialer(context: Context) {
        safeStart(context, Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openCamera(context: Context) {
        safeStart(
            context,
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openAppInfo(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        safeStart(context, intent)
    }

    fun requestUninstall(context: Context, packageName: String) {
        @Suppress("DEPRECATION")
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        safeStart(context, intent)
    }

    fun openHomeSettings(context: Context) {
        safeStart(
            context,
            Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * Öffnet die Notification-Shade. Nutzt Reflection auf StatusBarManager (offizielle API ist
     * @hide). Funktioniert auf praktisch allen Android-Versionen, erfordert EXPAND_STATUS_BAR
     * Permission (normal, auto-granted).
     */
    fun expandNotifications(context: Context) {
        try {
            val sbm = context.getSystemService("statusbar") ?: return
            val cls = Class.forName("android.app.StatusBarManager")
            val method = cls.getMethod("expandNotificationsPanel")
            method.invoke(sbm)
        } catch (_: Throwable) {
            // Stillschweigend ignorieren — System hat die API umbenannt o.ä.
        }
    }

    private fun safeStart(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(context, "App nicht gefunden")
        }
    }

    private fun toast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}

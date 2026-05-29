package de.dm.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import de.dm.launcher.domain.AppInfo
import java.text.Collator
import java.util.Locale

class AppRepository(private val context: Context) {

    fun loadLaunchableApps(): List<AppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val ownPackage = context.packageName
        val resolved = pm.queryIntentActivities(mainIntent, 0)
        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }
        return resolved
            .asSequence()
            .map { resolveInfo ->
                AppInfo(
                    packageName = resolveInfo.activityInfo.packageName,
                    label = resolveInfo.loadLabel(pm).toString()
                )
            }
            .distinctBy { it.packageName }
            .filter { it.packageName != ownPackage }
            .sortedWith(compareBy(collator) { it.label })
            .toList()
    }

    fun getLabel(packageName: String): String? = try {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(info).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    fun isInstalled(packageName: String): Boolean = try {
        context.packageManager.getApplicationInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

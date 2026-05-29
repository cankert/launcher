package de.dm.launcher.domain

data class AppInfo(
    val packageName: String,
    val label: String
) {
    val sortKey: String get() = label.lowercase()
}

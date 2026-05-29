package de.dm.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "launcher_prefs")

class PreferencesRepository(private val context: Context) {

    private val pinnedKey = stringPreferencesKey("pinned_packages_ordered")
    private val askedDefaultKey = booleanPreferencesKey("asked_default_launcher")
    private val askedNotifKey = booleanPreferencesKey("asked_notif_access")

    val pinnedPackages: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[pinnedKey]?.split(SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()
    }

    suspend fun togglePin(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[pinnedKey]
                ?.split(SEPARATOR)
                ?.filter { it.isNotEmpty() }
                ?.toMutableList()
                ?: mutableListOf()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            prefs[pinnedKey] = current.joinToString(SEPARATOR)
        }
    }

    suspend fun setPinned(packages: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[pinnedKey] = packages.joinToString(SEPARATOR)
        }
    }

    val askedDefaultLauncher: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[askedDefaultKey] ?: false
    }

    suspend fun setAskedDefaultLauncher() {
        context.dataStore.edit { prefs ->
            prefs[askedDefaultKey] = true
        }
    }

    val askedNotifAccess: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[askedNotifKey] ?: false
    }

    suspend fun setAskedNotifAccess() {
        context.dataStore.edit { prefs ->
            prefs[askedNotifKey] = true
        }
    }

    companion object {
        private const val SEPARATOR = "|"
    }
}

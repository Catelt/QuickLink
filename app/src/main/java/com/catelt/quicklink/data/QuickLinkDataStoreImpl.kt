package com.catelt.quicklink.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

private val Context.dataStore by preferencesDataStore(name = QuickLinkDataStoreImpl.LINKS_KEY_NAME)

class QuickLinkDataStoreImpl(private val context: Context) : QuickLinkDataStore {
    companion object {
        internal const val LINKS_KEY_NAME = "saved_links"
        private const val LINKS_JSON_KEY_NAME = "saved_links_json"
        private val LINKS_KEY = stringSetPreferencesKey(LINKS_KEY_NAME)
        private val LINKS_JSON_KEY = stringPreferencesKey(LINKS_JSON_KEY_NAME)
    }

    private val gson = Gson()

    private val listType = object : TypeToken<List<QuickLinkDataStore.StoredLink>>() {}.type

    override val links: Flow<List<QuickLinkDataStore.StoredLink>> = context.dataStore.data.transform { preferences ->
        val json = preferences[LINKS_JSON_KEY]
        if (json != null) {
            val list = runCatching {
                gson.fromJson<List<QuickLinkDataStore.StoredLink>>(json, listType)
            }.getOrElse { emptyList() }
                .sortedByDescending { it.timestamp }
            emit(list)
        } else {
            // Check legacy data and migrate if present
            val legacySet = preferences[LINKS_KEY]
            if (!legacySet.isNullOrEmpty()) {
                val migrated = legacySet.map { url ->
                    QuickLinkDataStore.StoredLink(url = url, timestamp = 0L)
                }
                // Persist migration and remove old key
                context.dataStore.edit { prefs ->
                    prefs[LINKS_JSON_KEY] = gson.toJson(migrated, listType)
                    if (prefs.contains(LINKS_KEY)) {
                        prefs.remove(LINKS_KEY)
                    }
                }
                emit(migrated.sortedByDescending { it.timestamp })
            } else {
                emit(emptyList())
            }
        }
    }

    override suspend fun saveLink(link: String) {
        context.dataStore.edit { preferences ->
            val now = System.currentTimeMillis()

            val currentList: List<QuickLinkDataStore.StoredLink> =
                preferences[LINKS_JSON_KEY]?.let { json ->
                    runCatching {
                        gson.fromJson<List<QuickLinkDataStore.StoredLink>>(json, listType)
                    }.getOrElse { emptyList() }
                } ?: run {
                    // Migrate legacy set to list
                    val legacySet = preferences[LINKS_KEY] ?: emptySet()
                    legacySet.map { url -> QuickLinkDataStore.StoredLink(url, 0L) }
                }

            val updated = buildList {
                // Remove existing entry if present
                currentList.filterNot { it.url == link }.forEach { add(it) }
                // Add new entry with current timestamp
                add(QuickLinkDataStore.StoredLink(url = link, timestamp = now))
            }.sortedByDescending { it.timestamp }

            preferences[LINKS_JSON_KEY] = gson.toJson(updated, listType)
            // Optionally clear legacy key after migration
            if (preferences.contains(LINKS_KEY)) {
                preferences.remove(LINKS_KEY)
            }
        }
    }

    override suspend fun removeLink(link: String) {
        context.dataStore.edit { preferences ->
            val currentList: List<QuickLinkDataStore.StoredLink> =
                preferences[LINKS_JSON_KEY]?.let { json ->
                    runCatching {
                        gson.fromJson<List<QuickLinkDataStore.StoredLink>>(json, listType)
                    }.getOrElse { emptyList() }
                } ?: emptyList()

            val updated = currentList.filterNot { it.url == link }
            preferences[LINKS_JSON_KEY] = gson.toJson(updated, listType)
        }
    }
}
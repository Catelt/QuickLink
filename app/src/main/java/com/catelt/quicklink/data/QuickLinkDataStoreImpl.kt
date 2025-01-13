package com.catelt.quicklink.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = QuickLinkDataStoreImpl.LINKS_KEY_NAME)

class QuickLinkDataStoreImpl(private val context: Context) : QuickLinkDataStore {
    companion object {
        internal const val LINKS_KEY_NAME = "saved_links"
        private val LINKS_KEY = stringSetPreferencesKey(LINKS_KEY_NAME)
    }

    override val links: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[LINKS_KEY] ?: emptySet()
    }

    override suspend fun saveLink(link: String) {
        context.dataStore.edit { preferences ->
            val currentLinks = preferences[LINKS_KEY] ?: emptySet()
            preferences[LINKS_KEY] = currentLinks + link
        }
    }

    override suspend fun removeLink(link: String) {
        context.dataStore.edit { preferences ->
            val currentLinks = (preferences[LINKS_KEY] ?: emptySet()).toMutableSet()
            currentLinks.remove(link)
            preferences[LINKS_KEY] = currentLinks
        }
    }
}
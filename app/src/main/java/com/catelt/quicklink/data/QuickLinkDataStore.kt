package com.catelt.quicklink.data

import kotlinx.coroutines.flow.Flow

interface QuickLinkDataStore {
    val links: Flow<Set<String>>
    suspend fun saveLink(link: String)

    suspend fun removeLink(link: String)
}
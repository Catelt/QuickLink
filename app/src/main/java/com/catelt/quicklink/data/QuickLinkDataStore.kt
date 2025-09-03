package com.catelt.quicklink.data

import kotlinx.coroutines.flow.Flow

interface QuickLinkDataStore {
    data class StoredLink(
        val url: String,
        val timestamp: Long,
    )

    val links: Flow<List<StoredLink>>

    suspend fun saveLink(link: String)

    suspend fun removeLink(link: String)
}
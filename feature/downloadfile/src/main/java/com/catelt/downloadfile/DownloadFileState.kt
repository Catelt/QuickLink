package com.catelt.downloadfile

sealed class DownloadFileState {
    data object Idle : DownloadFileState()
    data class Downloading(
        val process: Int,
        val isIndeterminate: Boolean = false,
        val downloadedBytes: Long = 0,
    ) : DownloadFileState()

    data object Completed : DownloadFileState()
    data class Error(val message: String) : DownloadFileState()
}
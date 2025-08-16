package com.catelt.quicklink.presentation.component

/**
 * Enum representing all available dynamic features in the app.
 * This centralizes the configuration for all dynamic feature modules.
 */
enum class DynamicFeature(
    val moduleName: String,
    val featureName: String,
    val implementationClassName: String
) {
    DOWNLOAD_FILE(
        moduleName = "downloadfile",
        featureName = "File Downloader",
        implementationClassName = "com.catelt.downloadfile.DownloadFileImpl"
    )
} 
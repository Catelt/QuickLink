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
    QR_SCANNER(
        moduleName = "scanqr",
        featureName = "QR Scanner",
        implementationClassName = "com.catelt.scanqr.QRScannerImpl"
    ),
    DEEPLINK(
        moduleName = "deeplink",
        featureName = "Deeplink",
        implementationClassName = "com.catelt.deeplink.DeeplinkImpl"
    ),
    QR_CODE(
        moduleName = "qrcode",
        featureName = "QR Code Generator",
        implementationClassName = "com.catelt.qrcode.QrCodeImpl"
    ),
    DOWNLOAD_FILE(
        moduleName = "downloadfile",
        featureName = "File Downloader",
        implementationClassName = "com.catelt.downloadfile.DownloadFileImpl"
    )
} 
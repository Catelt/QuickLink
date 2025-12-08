package com.catelt.quicklink.presentation.model

import androidx.annotation.DrawableRes
import com.catelt.quicklink.R

enum class Screen(
    val title: String,
    @DrawableRes val icon: Int,
) {
    Deeplink("Deeplink", R.drawable.baseline_deeplink),
    QrCode("QR Code", R.drawable.baseline_qr_code),
    ScanQr("QR & Barcode Scanner", R.drawable.baseline_qr_code_scan),
    ObjectDetection("Object Detection", R.drawable.baseline_object_detection),
    DownloadFile("Download File", R.drawable.baseline_download)
}
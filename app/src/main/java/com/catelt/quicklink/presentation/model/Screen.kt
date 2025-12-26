package com.catelt.quicklink.presentation.model

import com.catelt.quicklink.R

enum class Screen(
    val title: String,
    val icon: Int,
) {
    Deeplink("Deeplink", R.drawable.baseline_deeplink),
    QrCode("QR Code", R.drawable.baseline_qr_code),
    ScanQr("QR & Barcode Scanner", R.drawable.baseline_qr_code_scan),
}
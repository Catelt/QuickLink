package com.catelt.qrcode

import androidx.compose.runtime.Composable
import com.catelt.quicklink.presentation.component.DynamicFeatureComposable
import com.catelt.quicklink.presentation.viewmodel.QuickLinkViewModel

/**
 * Implementation of DynamicFeatureComposable that provides access to the QrCodeScreen composable.
 * This class allows the main app to access the QR code generation functionality without direct compilation dependencies.
 */
class QrCodeImpl : DynamicFeatureComposable {
    
    @Composable
    override fun CreateScreen(viewModel: QuickLinkViewModel) {
        // Call the actual QrCodeScreen composable
        QrCodeScreen(viewModel = viewModel)
    }
} 
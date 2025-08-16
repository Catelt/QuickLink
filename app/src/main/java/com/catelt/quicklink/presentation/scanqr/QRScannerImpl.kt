package com.catelt.quicklink.presentation.scanqr

import androidx.compose.runtime.Composable
import com.catelt.quicklink.presentation.component.DynamicFeatureComposable
import com.catelt.quicklink.presentation.viewmodel.QuickLinkViewModel

/**
 * Implementation of DynamicFeatureComposable that provides access to the ScanQRScreen composable.
 * This class allows the main app to access the QR scanner functionality without direct compilation dependencies.
 */
class QRScannerImpl : DynamicFeatureComposable {
    
    @Composable
    override fun CreateScreen(viewModel: QuickLinkViewModel) {
        // Call the actual ScanQRScreen composable
        ScanQRScreen(viewModel = viewModel)
    }
} 
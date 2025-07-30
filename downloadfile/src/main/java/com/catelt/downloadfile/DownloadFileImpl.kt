package com.catelt.downloadfile

import androidx.compose.runtime.Composable
import com.catelt.quicklink.presentation.component.DynamicFeatureComposable
import com.catelt.quicklink.presentation.viewmodel.QuickLinkViewModel

/**
 * Implementation of DynamicFeatureComposable that provides access to the DownloadFileScreen composable.
 * This class allows the main app to access the file download functionality without direct compilation dependencies.
 */
class DownloadFileImpl : DynamicFeatureComposable {
    
    @Composable
    override fun CreateScreen(viewModel: QuickLinkViewModel) {
        // Create a DownloadFileViewModel instance and call the DownloadFileScreen composable
        // Note: We need to create a DownloadFileViewModel here since the interface expects QuickLinkViewModel
        // but DownloadFileScreen needs DownloadFileViewModel
        val downloadFileViewModel = DownloadFileViewModel()
        DownloadFileScreen(viewModel = downloadFileViewModel)
    }
} 
package com.catelt.deeplink

import androidx.compose.runtime.Composable
import com.catelt.quicklink.presentation.component.DynamicFeatureComposable
import com.catelt.quicklink.presentation.viewmodel.QuickLinkViewModel

/**
 * Implementation of DynamicFeatureComposable that provides access to the DeeplinkScreen composable.
 * This class allows the main app to access the deeplink functionality without direct compilation dependencies.
 */
class DeeplinkImpl : DynamicFeatureComposable {
    
    @Composable
    override fun CreateScreen(viewModel: QuickLinkViewModel) {
        // Call the actual DeeplinkScreen composable
        DeeplinkScreen(viewModel = viewModel)
    }
} 
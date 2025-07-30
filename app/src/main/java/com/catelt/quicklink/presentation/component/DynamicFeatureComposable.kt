package com.catelt.quicklink.presentation.component

import androidx.compose.runtime.Composable
import com.catelt.quicklink.presentation.viewmodel.QuickLinkViewModel

/**
 * Generic interface for dynamic feature modules that expose a Composable screen.
 */
interface DynamicFeatureComposable {
    @Composable
    fun CreateScreen(viewModel: QuickLinkViewModel)
} 
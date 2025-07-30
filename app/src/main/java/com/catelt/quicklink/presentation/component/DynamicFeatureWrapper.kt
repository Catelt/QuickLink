package com.catelt.quicklink.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catelt.quicklink.presentation.viewmodel.QuickLinkViewModel
import com.catelt.quicklink.utils.DynamicModuleLoader
import com.catelt.quicklink.utils.DynamicFeatureFactory

/**
 * Generic wrapper for dynamic feature modules that handles loading, error states, and rendering.
 * @param dynamicFeature The dynamic feature to load and render
 * @param viewModel The QuickLinkViewModel instance
 */
@Composable
fun DynamicFeatureWrapper(
    dynamicFeature: DynamicFeature,
    viewModel: QuickLinkViewModel
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var feature by remember { mutableStateOf<DynamicFeatureComposable?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryTrigger) {
        try {
            val result = DynamicModuleLoader.loadModule(context, dynamicFeature.moduleName)
            result.fold(
                onSuccess = {
                    feature = DynamicFeatureFactory.createDynamicFeature(dynamicFeature)
                    if (feature == null) {
                        error = "${dynamicFeature.featureName} module loaded but implementation not found. " +
                                "Make sure the implementation class exists in the ${dynamicFeature.moduleName} module."
                    }
                },
                onFailure = { exception ->
                    error = "Failed to load ${dynamicFeature.featureName} module: ${exception.message}"
                }
            )
        } catch (e: Exception) {
            error = "Failed to load ${dynamicFeature.featureName} module: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator()
            }
            error != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = error!!,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = {
                            isLoading = true
                            error = null
                            feature = null
                            retryTrigger++
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
            feature != null -> {
                // Successfully loaded the feature, render it
                feature!!.CreateScreen(viewModel)
            }
            else -> {
                Text(
                    text = "${dynamicFeature.featureName} module not available",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
package com.catelt.quicklink.presentation.deeplink

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.catelt.quicklink.presentation.viewmodel.QuickLinkViewModel
import com.catelt.quicklink.presentation.component.InputLinkComponent
import com.catelt.quicklink.presentation.component.StoredLinksComponent

@Composable
fun DeeplinkScreen(
    viewModel: QuickLinkViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        val uiState by viewModel.uiState.collectAsState()

        InputLinkComponent(
            value = viewModel.deeplinkInput,
            labelButton = "OPEN LINK",
            onValueChange = { viewModel.updateInput(it) },
            onClickButton = {
                viewModel.openLink()
            }
        )

        StoredLinksComponent(
            data = uiState.links,
            onPlayClick = { url ->
                viewModel.openSpecificLink(url)
            },
            onCopyToClipboardClick = { url ->
                viewModel.copyToClipboard(url)
            },
            onDeletedClick = { url ->
                viewModel.removeLink(url)
            }
        )
    }
} 
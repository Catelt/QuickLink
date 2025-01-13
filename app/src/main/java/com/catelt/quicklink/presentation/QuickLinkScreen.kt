package com.catelt.quicklink.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.catelt.quicklink.R
import com.catelt.quicklink.presentation.component.InputLinkComponent
import com.catelt.quicklink.presentation.component.StoredLinksComponent


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLinkScreen(
    viewModel: QuickLinkViewModel,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
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
}
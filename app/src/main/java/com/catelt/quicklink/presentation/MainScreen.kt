package com.catelt.quicklink.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.catelt.quicklink.presentation.component.DynamicFeature
import com.catelt.quicklink.presentation.component.DynamicFeatureWrapper
import com.catelt.quicklink.presentation.deeplink.DeeplinkScreen
import com.catelt.quicklink.presentation.model.Screen
import com.catelt.quicklink.presentation.qrcode.QrCodeScreen
import com.catelt.quicklink.presentation.scanqr.ScanQRScreen
import com.catelt.quicklink.presentation.viewmodel.QuickLinkViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: QuickLinkViewModel,
) {
    var selectedScreen by remember { mutableStateOf(Screen.Deeplink) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        gesturesEnabled = false,
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                selectedScreen = selectedScreen,
                onSelectScreen = { value ->
                    selectedScreen = value
                    scope.launch {
                        drawerState.close()
                    }
                })
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedScreen.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding)
                    .padding(innerPadding)
                    .imePadding()
            ) {
                when (selectedScreen) {
                    Screen.Deeplink -> DeeplinkScreen(viewModel)
                    Screen.QrCode -> QrCodeScreen(viewModel)
                    Screen.ScanQr -> ScanQRScreen(viewModel)
                    Screen.DownloadFile -> DynamicFeatureWrapper(
                        dynamicFeature = DynamicFeature.DOWNLOAD_FILE,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
    selectedScreen: Screen,
    onSelectScreen: (Screen) -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.sizeIn(maxWidth = 230.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Screen.entries.forEach { screen ->
            DrawerItem(
                screen.title,
                screen.icon,
                screen,
                selectedScreen,
                onSelectScreen
            )
        }
    }
}

@Composable
private fun DrawerItem(
    text: String,
    @DrawableRes icon: Int,
    screen: Screen,
    selectedScreen: Screen,
    onSelectScreen: (Screen) -> Unit
) {
    NavigationDrawerItem(
        icon = {
            Icon(painterResource(icon), contentDescription = text)
        },
        label = {
            Text(text, style = MaterialTheme.typography.bodyLarge)
        },
        selected = selectedScreen == screen,
        onClick = {
            onSelectScreen(screen)
        }
    )
}
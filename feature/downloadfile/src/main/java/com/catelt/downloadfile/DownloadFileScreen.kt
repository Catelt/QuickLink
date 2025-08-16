package com.catelt.downloadfile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.catelt.component.BaseOutlinedTextField
import java.util.Locale

@Composable
fun DownloadFileScreen(
    viewModel: DownloadFileViewModel = viewModel()
) {
    val context = LocalContext.current
    val downloadState by viewModel.downloadState.collectAsState()

    val url by viewModel.urlText.collectAsState()
    val filename by viewModel.filenameText.collectAsState()


    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    viewModel.recoverExistingDownloads(context)
                }
                Lifecycle.Event.ON_DESTROY -> {
                    viewModel.unregisterReceiver(context)
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle permissions
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            emptyArray()
        } else {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    val hasStoragePermission = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                true // Android 10+ uses scoped storage
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val hasNotificationPermission = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { entry ->
            when (entry.key) {
                Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                    hasStoragePermission.value = entry.value
                }

                Manifest.permission.POST_NOTIFICATIONS -> {
                    hasNotificationPermission.value = entry.value
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Download File",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            BaseOutlinedTextField(
                value = url,
                onValueChange = {
                    viewModel.setUrlText(it)
                },
                label = "URL",
                placeholder = "Enter URL",
                autoFocus = false,
                maxLines = 6,
            )

            Spacer(modifier = Modifier.height(8.dp))

            BaseOutlinedTextField(
                value = filename,
                onValueChange = {
                    viewModel.setFilenameText(it)
                },
                label = "Filename",
                placeholder = "Enter filename with extension",
                autoFocus = false,
                showTrailingIcon = false,
                maxLines = 2,
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (downloadState) {
                is DownloadFileState.Idle -> {
                    Button(
                        onClick = {
                            if (url.isEmpty() || filename.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "URL and filename cannot be empty",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            // Check and request permission if needed
                            if (!hasNotificationPermission.value || !hasStoragePermission.value) {
                                permissionLauncher.launch(requiredPermissions)
                                return@Button
                            }

                            viewModel.startDownload(context, url, filename)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Download")
                    }
                }

                is DownloadFileState.Downloading -> {
                    val state = (downloadState as DownloadFileState.Downloading)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (state.isIndeterminate) {
                            // Indeterminate progress - content length unknown
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )

                            // Format downloaded bytes into a readable string
                            val downloadedText = when {
                                state.downloadedBytes < 1024 -> "${state.downloadedBytes} B"
                                state.downloadedBytes < 1024 * 1024 -> "${state.downloadedBytes / 1024} KB"
                                else -> String.format(
                                    Locale.getDefault(),
                                    "%.2f MB",
                                    state.downloadedBytes / (1024.0 * 1024.0)
                                )
                            }

                            Text(
                                text = "Downloaded: $downloadedText (Will continue in background if app is closed)",
                                modifier = Modifier.padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            // Determinate progress - we know the content length
                            LinearProgressIndicator(
                                progress = { state.process / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )

                            Text(
                                text = "${state.process}% (Will continue in background if app is closed)",
                                modifier = Modifier.padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.cancelDownload(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Cancel")
                        }
                    }
                }

                is DownloadFileState.Completed -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Download Complete!",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "File saved to Downloads folder",
                                    modifier = Modifier.padding(top = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.resetDownload(true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text("Download Another File")
                        }
                    }
                }

                is DownloadFileState.Error -> {
                    val errorMessage = (downloadState as DownloadFileState.Error).message

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Download Failed",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = errorMessage,
                                modifier = Modifier.padding(top = 8.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.resetDownload(false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
} 
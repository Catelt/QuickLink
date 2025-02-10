package com.catelt.quicklink.presentation.qr_code

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.catelt.quicklink.R
import com.catelt.quicklink.presentation.component.InputLinkComponent
import com.catelt.quicklink.presentation.component.StoredLinksComponent
import com.catelt.quicklink.presentation.viewmodel.QuickLinkViewModel
import com.catelt.quicklink.utils.QrCodeUtil

@Composable
fun QrCodeScreen(
    viewModel: QuickLinkViewModel,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var inputLink by remember { mutableStateOf("") }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }

    fun setInputLink(url: String) {
        inputLink = url
        qrCodeBitmap = QrCodeUtil.generateQrCodeBitmap(url, 200)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (qrCodeBitmap != null) {
                Image(
                    modifier = Modifier.size(200.dp),
                    bitmap = qrCodeBitmap!!.asImageBitmap(),
                    contentDescription = "QR Code",
                )

                IconButton(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onClick = {
                        if (qrCodeBitmap != null) {
                            val success =
                                QrCodeUtil.saveQrCodeToGallery(context, qrCodeBitmap!!)
                            if (success) {
                                viewModel.showToast("QR Code saved to gallery!")
                            } else {
                                viewModel.showToast("Failed to save QR Code")
                            }
                        } else {
                            viewModel.showToast("Please enter a valid URL")
                        }
                    }
                ) {
                    Icon(
                        painterResource(R.drawable.baseline_download),
                        contentDescription = "Download"
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .background(Color.Gray.copy(alpha = 0.2f))
                        .size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "QR Code will appear here")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        InputLinkComponent(
            value = inputLink,
            labelButton = "Generate QR Code",
            onValueChange = { inputLink = it },
            onClickButton = {
                if (inputLink.isNotBlank()) {
                    setInputLink(inputLink)
                    viewModel.saveLink(inputLink)
                } else {
                    viewModel.showToast("Please enter a valid URL")
                }
            }
        )
        StoredLinksComponent(
            data = uiState.links,
            onPlayClick = { url ->
                setInputLink(url)
            },
            onCopyToClipboardClick = { url ->
                viewModel.copyToClipboard(url)
            },
            onDeletedClick = { url ->
                viewModel.removeLink(url)
                qrCodeBitmap = null
            }
        )
    }
}



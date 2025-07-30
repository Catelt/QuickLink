package com.catelt.quicklink

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.catelt.quicklink.data.QuickLinkDataStore
import com.catelt.quicklink.data.QuickLinkDataStoreImpl
import com.catelt.quicklink.presentation.MainScreen
import com.catelt.quicklink.presentation.viewmodel.QuickLinkEvent
import com.catelt.quicklink.presentation.viewmodel.QuickLinkViewModel
import com.catelt.quicklink.ui.theme.QuickLinkTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dataStore: QuickLinkDataStore = QuickLinkDataStoreImpl(this)
        val quickLinkViewModel: QuickLinkViewModel by viewModels {
            QuickLinkViewModel.createFactory(
                dataStore = dataStore
            )
        }

        lifecycleScope.launch {
            quickLinkViewModel.quickLinkEvent.collect { event ->
                when (event) {
                    is QuickLinkEvent.OpenLink -> {
                        if (handleDeepLink(event.url)) {
                            quickLinkViewModel.saveLink(event.url)
                        }
                    }

                    is QuickLinkEvent.CopyToClipboard -> handleCopyToClipboard(event.url)

                    is QuickLinkEvent.ShareText -> handleShareText(event.text)

                    is QuickLinkEvent.ShowToast -> showToast(event.message)
                }
            }
        }

        setContent {
            QuickLinkTheme {
                MainScreen(
                    viewModel = quickLinkViewModel,
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    private fun handleDeepLink(url: String): Boolean {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
            return true
        } catch (e: Exception) {
            showToast("Invalid URL or no app to handle it!")
        }
        return false
    }

    private fun handleCopyToClipboard(url: String): Boolean {
        try {
            val clipboard =
                this.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    ?: throw Exception("Clipboard null")
            val clip = ClipData.newPlainText("URL", url)
            clipboard.setPrimaryClip(clip)
            showToast("Copied to clipboard!")
            return true
        } catch (e: Exception) {
            showToast("Copy to Clipboard Failed!")
        }
        return false
    }

    private fun handleShareText(text: String): Boolean {
        try {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
            return true
        } catch (e: Exception) {
            showToast("Share Text Failed!")
        }
        return false
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}


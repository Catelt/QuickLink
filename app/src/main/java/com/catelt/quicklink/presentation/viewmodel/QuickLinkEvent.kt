package com.catelt.quicklink.presentation.viewmodel

sealed class QuickLinkEvent {
    data class OpenLink(val url: String): QuickLinkEvent()
    data class CopyToClipboard(val url: String): QuickLinkEvent()
    data class ShowToast(val message: String) : QuickLinkEvent()
}
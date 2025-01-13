package com.catelt.quicklink.presentation

sealed class QuickLinkEvent {
    data class OpenLink(val url: String): QuickLinkEvent()
    data class CopyToClipboard(val url: String): QuickLinkEvent()
    data class ShowError(val message: String) : QuickLinkEvent()
}
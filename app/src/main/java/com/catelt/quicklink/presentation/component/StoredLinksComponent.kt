package com.catelt.quicklink.presentation.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StoredLinksComponent(
    data: List<String>,
    onPlayClick: (String) -> Unit,
    onCopyToClipboardClick: (String) -> Unit,
    onDeletedClick: (String) -> Unit,
) {
    Text(
        "Stored Links",
        modifier = Modifier.padding(vertical = 8.dp),
        style = MaterialTheme.typography.titleLarge
    )

    val links = data.reversed()

    LazyColumn {
        items(links.size) { index ->
            val link = links[index]
            StoredLinkItem(
                link = links[index],
                onPlayClick = {
                    onPlayClick(link)
                },
                onCopyToClipboardClick = {
                    onCopyToClipboardClick(link)
                },
                onDeletedClick = {
                    onDeletedClick(link)
                },
            )
        }
    }
}
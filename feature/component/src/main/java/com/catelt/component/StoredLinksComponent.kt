package com.catelt.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.itemsIndexed

@Composable
fun StoredLinksComponent(
    modifier: Modifier = Modifier,
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

    LazyColumn(
        modifier = modifier
    ) {
        itemsIndexed(
            items = data,
            key = { _, item -> item }
        ) { _, link ->
            StoredLinkItem(
                modifier = Modifier.animateItem(),
                link = link,
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
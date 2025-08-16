package com.catelt.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun StoredLinkItem(
    link: String,
    onPlayClick: () -> Unit = {},
    onCopyToClipboardClick: () -> Unit = {},
    onDeletedClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .sizeIn(minHeight = 50.dp)
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = link,
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 3,
            modifier = Modifier.weight(1f)
        )

        ItemButton(
            clickable = onPlayClick,
            icon = R.drawable.baseline_play_arrow,
            contentDescription = "Open URL",
        )

        ItemButton(
            clickable = onCopyToClipboardClick,
            icon = R.drawable.baseline_content_copy,
            contentDescription = "Copy to clipboard",
        )

        ItemButton(
            clickable = onDeletedClick,
            icon = R.drawable.outline_delete,
            contentDescription = "Remove Link",
        )
    }
}

@Composable
private fun ItemButton(
    clickable: () -> Unit,
    @DrawableRes icon: Int,
    contentDescription: String,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .background(
                color = Color.Transparent,
                shape = shape
            )
            .clip(shape)
            .clickable {
                clickable()
            }
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
        )
    }
}

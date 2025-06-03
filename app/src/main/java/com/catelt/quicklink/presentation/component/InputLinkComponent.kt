package com.catelt.quicklink.presentation.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InputLinkComponent(
    value: String,
    label: String = "Enter URL",
    labelButton: String,
    onValueChange: (String) -> Unit,
    onClickButton: () -> Unit,
) {
    BaseOutlinedTextField(
        value = value,
        label = label,
        maxLines = 6,
        onValueChange = onValueChange
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = onClickButton,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(labelButton)
    }
}
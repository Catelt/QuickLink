package com.catelt.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction

@Composable
fun BaseOutlinedTextField(
    value: String,
    label: String,
    placeholder: String? = null,
    maxLines: Int = 1,
    showTrailingIcon: Boolean = true,
    autoFocus: Boolean = true,
    onValueChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it) },
        label = { Text(label) },
        placeholder = { if (placeholder != null) Text(placeholder) },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
        trailingIcon = {
            if (value.isNotEmpty() && showTrailingIcon) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear text"
                    )
                }
            }
        }
    )
}
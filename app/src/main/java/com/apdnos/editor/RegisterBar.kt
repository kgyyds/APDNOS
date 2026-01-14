package com.apdnos.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun RegisterBar(usage: RegisterUsage) {
    val text = buildString {
        if (usage.x.isEmpty() && usage.w.isEmpty() && usage.special.isEmpty()) {
            append("REG: (none)")
            return@buildString
        }
        append("REG:")
        if (usage.x.isNotEmpty()) {
            append(" x: ")
            append(usage.x.joinToString(" "))
        }
        if (usage.w.isNotEmpty()) {
            append(" | w: ")
            append(usage.w.joinToString(" "))
        }
        if (usage.special.isNotEmpty()) {
            append(" | special: ")
            append(usage.special.joinToString(" "))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

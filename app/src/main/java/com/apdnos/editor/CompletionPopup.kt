package com.apdnos.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.offset

@Composable
fun CompletionPopup(
    items: List<CompletionItem>,
    selectedIndex: Int,
    maxWidth: Dp,
    maxHeight: Dp,
    offset: IntOffset,
    onSelect: (CompletionItem) -> Unit
) {
    if (items.isEmpty()) return
    val scrollState = rememberScrollState()
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .zIndex(1f)
            .offset { offset }
            .widthIn(max = maxWidth)
            .heightIn(max = maxHeight)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(scrollState)
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val background = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                } else {
                    Color.Transparent
                }
                Text(
                    text = item.label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(background)
                        .clickable { onSelect(item) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

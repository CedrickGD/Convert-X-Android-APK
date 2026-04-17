package com.cedrickgd.convertx.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cedrickgd.convertx.domain.FileType
import com.cedrickgd.convertx.domain.Format
import com.cedrickgd.convertx.ui.components.SectionHeader
import com.cedrickgd.convertx.ui.theme.BrandTokens

@Composable
fun FormatPickerGrouped(
    selected: Format?,
    inputTypes: Set<FileType>,
    onSelect: (Format) -> Unit,
    modifier: Modifier = Modifier
) {
    val allowed = Format.entries.filter { format ->
        format.accepts.any { it in inputTypes }
    }
    if (allowed.isEmpty()) return

    val videos = allowed.filter { it.category == FileType.VIDEO }
    val images = allowed.filter { it.category == FileType.IMAGE }
    val audios = allowed.filter { it.category == FileType.AUDIO }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(text = "Format")
        if (videos.isNotEmpty()) {
            FormatGroup(title = "Video", formats = videos, selected = selected, onSelect = onSelect)
        }
        if (images.isNotEmpty()) {
            FormatGroup(title = "Image", formats = images, selected = selected, onSelect = onSelect)
        }
        if (audios.isNotEmpty()) {
            FormatGroup(title = "Audio", formats = audios, selected = selected, onSelect = onSelect)
        }
    }
}

@Composable
private fun FormatGroup(
    title: String,
    formats: List<Format>,
    selected: Format?,
    onSelect: (Format) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(formats, key = { it.name }) { format ->
                FormatChip(
                    format = format,
                    selected = format == selected,
                    onClick = { onSelect(format) }
                )
            }
        }
    }
}

@Composable
private fun FormatChip(
    format: Format,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    val baseModifier = Modifier
        .height(38.dp)
        .clip(shape)
        .clickable(onClick = onClick)
    val decorated = if (selected) {
        baseModifier.background(BrandTokens.brandGradient)
    } else {
        baseModifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.outline,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ),
                shape = shape
            )
    }
    Box(
        modifier = decorated.padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = format.displayName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

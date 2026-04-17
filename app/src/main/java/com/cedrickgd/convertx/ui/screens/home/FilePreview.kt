package com.cedrickgd.convertx.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cedrickgd.convertx.domain.FileEntry
import com.cedrickgd.convertx.domain.FileType
import com.cedrickgd.convertx.util.formatBytes
import com.cedrickgd.convertx.util.formatDuration

@Composable
fun FilePreview(
    entry: FileEntry,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                val type = entry.detectedType
                if (type == FileType.IMAGE || type == FileType.VIDEO) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(entry.sourceUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = entry.displayName,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Icon(
                        imageVector = if (type == FileType.AUDIO) Icons.Outlined.Audiotrack
                        else Icons.Outlined.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = entry.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            MetadataGrid(entry = entry)
        }
    }
}

@Composable
private fun MetadataGrid(entry: FileEntry) {
    val meta = entry.metadata
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            MetaCell(label = "Type", value = entry.detectedType.name, modifier = Modifier.weight(1f))
            MetaCell(label = "Size", value = formatBytes(meta?.size ?: 0L), modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            MetaCell(label = "Codec", value = meta?.codec ?: "—", modifier = Modifier.weight(1f))
            MetaCell(
                label = "Resolution",
                value = meta?.resolution ?: "—",
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth()) {
            MetaCell(
                label = "Duration",
                value = meta?.duration?.let { formatDuration(it) } ?: "—",
                modifier = Modifier.weight(1f)
            )
            MetaCell(
                label = "Bitrate",
                value = meta?.bitrate ?: "—",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetaCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(end = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

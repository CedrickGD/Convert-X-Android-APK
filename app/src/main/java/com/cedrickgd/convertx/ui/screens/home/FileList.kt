package com.cedrickgd.convertx.ui.screens.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cedrickgd.convertx.domain.FileEntry
import com.cedrickgd.convertx.domain.FileStatus
import com.cedrickgd.convertx.domain.FileType
import com.cedrickgd.convertx.util.formatBytes
import com.cedrickgd.convertx.util.formatDuration

@Composable
fun FileList(
    files: List<FileEntry>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
    showRemove: Boolean = true
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(files, key = { it.id }) { entry ->
            FileRow(
                entry = entry,
                onRemove = { onRemove(entry.id) },
                showRemove = showRemove
            )
        }
    }
}

@Composable
private fun FileRow(
    entry: FileEntry,
    onRemove: () -> Unit,
    showRemove: Boolean
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                FileThumb(entry = entry)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = buildString {
                        append(formatBytes(entry.metadata?.size ?: 0L))
                        entry.metadata?.duration?.let { d ->
                            append(" · ")
                            append(formatDuration(d))
                        }
                        entry.metadata?.resolution?.let { r ->
                            append(" · ")
                            append(r)
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusChip(status = entry.status, error = entry.error)
                if (showRemove && entry.status != FileStatus.CONVERTING) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (entry.status == FileStatus.CONVERTING) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { entry.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

@Composable
private fun FileThumb(entry: FileEntry) {
    val context = LocalContext.current
    val type = entry.detectedType
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        if (type == FileType.IMAGE || type == FileType.VIDEO) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(entry.sourceUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp))
            )
        } else {
            val icon = when (type) {
                FileType.AUDIO -> Icons.Outlined.Audiotrack
                FileType.VIDEO -> Icons.Outlined.Movie
                else -> Icons.Outlined.Image
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatusChip(status: FileStatus, error: String?) {
    val (label, color) = when (status) {
        FileStatus.READY -> "Ready" to MaterialTheme.colorScheme.primary
        FileStatus.DETECTING -> "Reading" to MaterialTheme.colorScheme.secondary
        FileStatus.QUEUED -> "Queued" to MaterialTheme.colorScheme.secondary
        FileStatus.CONVERTING -> null to MaterialTheme.colorScheme.primary
        FileStatus.DONE -> "Done" to MaterialTheme.colorScheme.primary
        FileStatus.ERROR -> "Error" to MaterialTheme.colorScheme.error
        FileStatus.SKIPPED -> "Skipped" to MaterialTheme.colorScheme.outline
        FileStatus.IDLE -> "Idle" to MaterialTheme.colorScheme.outline
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.18f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (status) {
                FileStatus.CONVERTING -> AnimatedDots(color = color)
                FileStatus.DONE -> Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                FileStatus.ERROR -> Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = error,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                FileStatus.SKIPPED -> Icon(
                    imageVector = Icons.Outlined.RemoveCircleOutline,
                    contentDescription = error,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                else -> {}
            }
            if (label != null) {
                if (status == FileStatus.DONE || status == FileStatus.ERROR ||
                    status == FileStatus.SKIPPED
                ) {
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun AnimatedDots(color: Color) {
    val transition = rememberInfiniteTransition(label = "dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot-phase"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (i in 0..2) {
            val active = (phase.toInt() % 3) == i
            Box(
                modifier = Modifier
                    .size(if (active) 5.dp else 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
            if (i < 2) Spacer(Modifier.width(3.dp))
        }
    }
}

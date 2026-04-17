package com.cedrickgd.convertx.ui.screens.output

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cedrickgd.convertx.domain.FileEntry
import com.cedrickgd.convertx.domain.FileStatus
import com.cedrickgd.convertx.ui.components.GradientButton
import com.cedrickgd.convertx.ui.components.SectionHeader
import com.cedrickgd.convertx.util.formatBytes

@Composable
fun OutputPanel(
    files: List<FileEntry>,
    onStartOver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val successes = files.filter { it.status == FileStatus.DONE }
    val skipped = files.filter { it.status == FileStatus.SKIPPED }
    val errors = files.filter { it.status == FileStatus.ERROR }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (successes.isNotEmpty()) {
            SectionHeader(text = "Finished (${successes.size})")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                successes.forEach { entry ->
                    SuccessRow(entry = entry)
                }
            }
        }
        if (skipped.isNotEmpty()) {
            SectionHeader(text = "Skipped (${skipped.size})")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                skipped.forEach { entry ->
                    SkippedRow(entry = entry)
                }
            }
        }
        if (errors.isNotEmpty()) {
            SectionHeader(text = "Failed (${errors.size})")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                errors.forEach { entry ->
                    ErrorRow(entry = entry)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        GradientButton(
            onClick = onStartOver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Start over", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SuccessRow(entry: FileEntry) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(color = MaterialTheme.colorScheme.primary, icon = Icons.Outlined.CheckCircle)
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.outputName ?: entry.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatBytes(entry.outputSize),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            entry.outputUri?.let { uri ->
                IconButton(onClick = { shareFile(context, uri) }) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { openFile(context, uri) }) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SkippedRow(entry: FileEntry) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(
                color = MaterialTheme.colorScheme.outline,
                icon = Icons.Outlined.RemoveCircleOutline
            )
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.error ?: "Skipped",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorRow(entry: FileEntry) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(color = MaterialTheme.colorScheme.error, icon = Icons.Outlined.ErrorOutline)
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.error ?: "Failed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun StatusDot(
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun shareFile(context: android.content.Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = context.contentResolver.getType(uri) ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share file"))
    }
}

private fun openFile(context: android.content.Context, uri: Uri) {
    val mime = context.contentResolver.getType(uri) ?: "*/*"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        context.startActivity(intent)
    }
}

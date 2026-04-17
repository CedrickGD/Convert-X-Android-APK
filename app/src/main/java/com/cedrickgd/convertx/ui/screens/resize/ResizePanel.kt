package com.cedrickgd.convertx.ui.screens.resize

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.cedrickgd.convertx.domain.AppSettings
import com.cedrickgd.convertx.domain.Format
import com.cedrickgd.convertx.domain.ResizeMode
import com.cedrickgd.convertx.ui.components.SectionHeader
import com.cedrickgd.convertx.ui.theme.BrandTokens
import kotlin.math.roundToInt

private val ResizeFormats = listOf(Format.PNG, Format.JPG, Format.WEBP)

@Composable
fun ResizePanel(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    outputTreeUri: Uri?,
    onSetOutputTree: (Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(text = "Resize")
        ModeToggle(
            selected = settings.resizeMode,
            onSelect = { mode -> onUpdateSettings { it.copy(resizeMode = mode) } }
        )

        when (settings.resizeMode) {
            ResizeMode.PIXELS -> PixelsPanel(settings = settings, onUpdateSettings = onUpdateSettings)
            ResizeMode.PERCENTAGE -> PercentPanel(settings = settings, onUpdateSettings = onUpdateSettings)
        }

        FormatDropdown(
            selected = settings.resizeFormat,
            onSelect = { format -> onUpdateSettings { it.copy(resizeFormat = format) } }
        )

        OutputFolderPicker(
            outputTreeUri = outputTreeUri,
            onChange = onSetOutputTree
        )
    }
}

@Composable
private fun OutputFolderPicker(
    outputTreeUri: Uri?,
    onChange: (Uri?) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            onChange(uri)
        }
    }
    val label = outputTreeUri?.let { uri ->
        runCatching { DocumentFile.fromTreeUri(context, uri)?.name }.getOrNull()
    } ?: "Tap to pick"
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { launcher.launch(outputTreeUri) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Output folder",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Change",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ModeToggle(
    selected: ResizeMode,
    onSelect: (ResizeMode) -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ResizePill(
            label = "Pixels",
            selected = selected == ResizeMode.PIXELS,
            onClick = { onSelect(ResizeMode.PIXELS) },
            modifier = Modifier.weight(1f)
        )
        ResizePill(
            label = "Percentage",
            selected = selected == ResizeMode.PERCENTAGE,
            onClick = { onSelect(ResizeMode.PERCENTAGE) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ResizePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(
                if (selected) BrandTokens.brandGradient
                else Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PixelsPanel(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = settings.resizeWidth?.toString().orEmpty(),
                onValueChange = { raw ->
                    val parsed = raw.filter { it.isDigit() }.take(5).toIntOrNull()
                    onUpdateSettings { it.copy(resizeWidth = parsed) }
                },
                label = { Text("Width") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            OutlinedTextField(
                value = settings.resizeHeight?.toString().orEmpty(),
                onValueChange = { raw ->
                    val parsed = raw.filter { it.isDigit() }.take(5).toIntOrNull()
                    onUpdateSettings { it.copy(resizeHeight = parsed) }
                },
                label = { Text("Height") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lock aspect ratio",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Scales the missing dimension proportionally",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.keepAspect,
                onCheckedChange = { value -> onUpdateSettings { it.copy(keepAspect = value) } }
            )
        }
    }
}

@Composable
private fun PercentPanel(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Scale",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${settings.resizePercent}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = settings.resizePercent.toFloat(),
            onValueChange = { v ->
                onUpdateSettings { it.copy(resizePercent = v.roundToInt().coerceIn(10, 200)) }
            },
            valueRange = 10f..200f,
            steps = 37
        )
    }
}

@Composable
private fun FormatDropdown(
    selected: Format?,
    onSelect: (Format?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = selected?.displayName ?: "Same as input"
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Output format",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Box {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Same as input") },
                    onClick = {
                        onSelect(null)
                        expanded = false
                    }
                )
                ResizeFormats.forEach { format ->
                    DropdownMenuItem(
                        text = { Text(format.displayName) },
                        onClick = {
                            onSelect(format)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

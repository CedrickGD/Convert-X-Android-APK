package com.cedrickgd.convertx.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cedrickgd.convertx.data.AppContainer
import com.cedrickgd.convertx.domain.AppMode
import com.cedrickgd.convertx.domain.AppSettings
import com.cedrickgd.convertx.domain.AppView
import com.cedrickgd.convertx.domain.FileEntry
import com.cedrickgd.convertx.domain.FileStatus
import com.cedrickgd.convertx.domain.FileType
import com.cedrickgd.convertx.domain.Format
import com.cedrickgd.convertx.ui.components.AnimatedDropzone
import com.cedrickgd.convertx.ui.components.BigProgressBar
import com.cedrickgd.convertx.ui.components.GradientButton
import com.cedrickgd.convertx.ui.screens.output.OutputPanel
import com.cedrickgd.convertx.ui.screens.resize.ResizePanel
import com.cedrickgd.convertx.ui.theme.BrandTokens
import com.cedrickgd.convertx.ui.theme.ConvertXTheme
import com.cedrickgd.convertx.ui.theme.ThemeMode

@Composable
fun HomeScreen(
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(appContainer))
    val state by vm.uiState.collectAsStateWithLifecycle()
    HomeScaffold(
        state = state,
        onPickFiles = vm::onFilesPicked,
        onRemoveFile = vm::removeFile,
        onClearAll = vm::clearAll,
        onSetMode = vm::setMode,
        onSetFormat = vm::setFormat,
        onUpdateSettings = vm::updateSettings,
        onSetOutputName = vm::setOutputName,
        onSetOutputTree = vm::setOutputTree,
        onStartConvert = vm::startConvert,
        onStartResize = vm::startResize,
        onCancel = vm::cancel,
        modifier = modifier
    )
}

@Composable
internal fun HomeScaffold(
    state: HomeUiState,
    onPickFiles: (List<Uri>, List<String>, android.content.Context) -> Unit,
    onRemoveFile: (String) -> Unit,
    onClearAll: () -> Unit,
    onSetMode: (AppMode) -> Unit,
    onSetFormat: (Format) -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onSetOutputName: (String, String) -> Unit,
    onSetOutputTree: (Uri?) -> Unit,
    onStartConvert: () -> Unit,
    onStartResize: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            val names = uris.map { it.lastPathSegment?.substringAfterLast('/') ?: "file" }
            onPickFiles(uris, names, context)
        }
    }

    val pickFiles: () -> Unit = {
        pickLauncher.launch(arrayOf("image/*", "video/*", "audio/*"))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AnimatedContent(
            targetState = state.view,
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
            },
            label = "home-view"
        ) { view ->
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (view) {
                    AppView.IDLE -> IdleState(
                        mode = state.mode,
                        onSetMode = onSetMode,
                        onPickFiles = pickFiles
                    )
                    AppView.READY -> ReadyState(
                        state = state,
                        onRemoveFile = onRemoveFile,
                        onSetFormat = onSetFormat,
                        onUpdateSettings = onUpdateSettings,
                        onSetOutputName = onSetOutputName,
                        onSetOutputTree = onSetOutputTree,
                        onClearAll = onClearAll,
                        onStartConvert = onStartConvert,
                        onStartResize = onStartResize
                    )
                    AppView.CONVERTING -> ConvertingState(
                        state = state,
                        onRemoveFile = onRemoveFile,
                        onCancel = onCancel
                    )
                    AppView.DONE -> OutputPanel(
                        files = state.files,
                        onStartOver = onClearAll
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleState(
    mode: AppMode,
    onSetMode: (AppMode) -> Unit,
    onPickFiles: () -> Unit
) {
    ModeTabs(selected = mode, onSelect = onSetMode)
    val (title, subtitle, label) = if (mode == AppMode.CONVERT) {
        Triple(
            "Drop files to convert",
            "Video · Image · Audio — batch ready",
            "Pick files"
        )
    } else {
        Triple(
            "Drop images to resize",
            "PNG · JPG · WebP",
            "Pick images"
        )
    }
    AnimatedDropzone(
        onPickFiles = onPickFiles,
        title = title,
        subtitle = subtitle,
        buttonLabel = label
    )
}

@Composable
private fun ReadyState(
    state: HomeUiState,
    onRemoveFile: (String) -> Unit,
    onSetFormat: (Format) -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onSetOutputName: (String, String) -> Unit,
    onSetOutputTree: (Uri?) -> Unit,
    onClearAll: () -> Unit,
    onStartConvert: () -> Unit,
    onStartResize: () -> Unit
) {
    if (state.files.size == 1) {
        FilePreview(entry = state.files.first())
    } else {
        FileList(files = state.files, onRemove = onRemoveFile)
    }

    if (state.mode == AppMode.CONVERT) {
        val inputTypes = state.files.map { it.detectedType }.toSet()
        FormatPickerGrouped(
            selected = state.settings.selectedFormat,
            inputTypes = inputTypes,
            onSelect = onSetFormat
        )
        val selectedFormat = state.settings.selectedFormat
        val longestDuration = state.files.mapNotNull { it.metadata?.duration }.maxOrNull() ?: 0.0
        if (selectedFormat == Format.GIF && longestDuration > 0.0) {
            GifEditor(
                duration = longestDuration,
                trimStart = state.settings.trimStart,
                trimEnd = state.settings.trimEnd,
                onTrimChange = { start, end ->
                    onUpdateSettings { it.copy(trimStart = start, trimEnd = end) }
                },
                stripAudio = state.settings.stripAudio,
                onStripAudioChange = { value ->
                    onUpdateSettings { it.copy(stripAudio = value) }
                },
                gifColors = state.settings.gifColors,
                onGifColorsChange = { value ->
                    onUpdateSettings { it.copy(gifColors = value) }
                },
                gifFps = state.settings.gifFps ?: 15,
                onGifFpsChange = { value ->
                    onUpdateSettings { it.copy(gifFps = value) }
                },
                gifWidth = state.settings.gifWidth ?: 480,
                onGifWidthChange = { value ->
                    onUpdateSettings { it.copy(gifWidth = value) }
                },
                gifTargetSizeMb = state.settings.gifTargetSizeMb ?: 0,
                onGifTargetSizeMbChange = { value ->
                    onUpdateSettings { it.copy(gifTargetSizeMb = if (value == 0) null else value) }
                }
            )
        }
        if (selectedFormat != null && selectedFormat.category == FileType.VIDEO &&
            selectedFormat != Format.GIF
        ) {
            OutputSettings(
                files = state.files,
                quality = state.settings.quality,
                onQualityChange = { value ->
                    onUpdateSettings { it.copy(quality = value) }
                },
                outputTreeUri = state.outputTreeUri,
                onOutputTreeChange = onSetOutputTree,
                onFileNameChange = onSetOutputName
            )
            AdvancedSettings(
                format = selectedFormat,
                resolution = state.settings.resolution,
                onResolutionChange = { value ->
                    onUpdateSettings { it.copy(resolution = value) }
                },
                fps = state.settings.fps,
                onFpsChange = { value ->
                    onUpdateSettings { it.copy(fps = value) }
                },
                bitrate = state.settings.bitrate,
                onBitrateChange = { value ->
                    onUpdateSettings { it.copy(bitrate = value) }
                },
                preset = state.settings.preset,
                onPresetChange = { value ->
                    onUpdateSettings { it.copy(preset = value) }
                }
            )
        }
        ActionsRow(
            primaryLabel = actionLabel(
                count = compatibleCount(state.files, selectedFormat),
                mode = AppMode.CONVERT
            ),
            primaryEnabled = selectedFormat != null &&
                compatibleCount(state.files, selectedFormat) > 0,
            onBack = onClearAll,
            onPrimary = onStartConvert
        )
    } else {
        ResizePanel(
            settings = state.settings,
            onUpdateSettings = onUpdateSettings,
            outputTreeUri = state.outputTreeUri,
            onSetOutputTree = onSetOutputTree
        )
        val imageCount = state.files.count { it.detectedType == FileType.IMAGE }
        ActionsRow(
            primaryLabel = if (imageCount > 1) "Resize $imageCount images" else "Resize",
            primaryEnabled = imageCount > 0,
            onBack = onClearAll,
            onPrimary = onStartResize
        )
    }
}

@Composable
private fun ConvertingState(
    state: HomeUiState,
    onRemoveFile: (String) -> Unit,
    onCancel: () -> Unit
) {
    FileList(files = state.files, onRemove = onRemoveFile, showRemove = false)
    val overall = overallProgress(state.files)
    BigProgressBar(progress = overall)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        OutlinedButton(onClick = onCancel) {
            Text(text = if (state.isCancelling) "Cancelling…" else "Cancel")
        }
    }
}

@Composable
private fun ModeTabs(
    selected: AppMode,
    onSelect: (AppMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ModePill(
            label = "Convert",
            selected = selected == AppMode.CONVERT,
            onClick = { onSelect(AppMode.CONVERT) },
            modifier = Modifier.weight(1f)
        )
        ModePill(
            label = "Resize",
            selected = selected == AppMode.RESIZE,
            onClick = { onSelect(AppMode.RESIZE) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModePill(
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
                else androidx.compose.ui.graphics.Brush.linearGradient(
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
private fun ActionsRow(
    primaryLabel: String,
    primaryEnabled: Boolean,
    onBack: () -> Unit,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
        Spacer(modifier = Modifier.weight(1f))
        GradientButton(
            onClick = onPrimary,
            enabled = primaryEnabled
        ) {
            Text(
                text = primaryLabel,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun actionLabel(count: Int, mode: AppMode): String {
    val verb = if (mode == AppMode.CONVERT) "Convert" else "Resize"
    return if (count <= 1) verb else "$verb $count files"
}

private fun compatibleCount(files: List<FileEntry>, format: Format?): Int {
    if (format == null) return 0
    return files.count { entry ->
        entry.status != FileStatus.SKIPPED && entry.detectedType in format.accepts
    }
}

private fun overallProgress(files: List<FileEntry>): Float {
    val active = files.filter {
        it.status == FileStatus.CONVERTING || it.status == FileStatus.DONE ||
            it.status == FileStatus.QUEUED || it.status == FileStatus.ERROR
    }
    if (active.isEmpty()) return 0f
    val sum = active.sumOf { entry ->
        when (entry.status) {
            FileStatus.DONE -> 1.0
            FileStatus.CONVERTING -> entry.progress.toDouble()
            else -> 0.0
        }
    }
    return (sum / active.size).toFloat()
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0512)
@Composable
private fun HomeIdlePreview() {
    ConvertXTheme(themeMode = ThemeMode.DARK) {
        HomeScaffold(
            state = HomeUiState(view = AppView.IDLE, mode = AppMode.CONVERT),
            onPickFiles = { _, _, _ -> },
            onRemoveFile = {},
            onClearAll = {},
            onSetMode = {},
            onSetFormat = {},
            onUpdateSettings = {},
            onSetOutputName = { _, _ -> },
            onSetOutputTree = {},
            onStartConvert = {},
            onStartResize = {},
            onCancel = {}
        )
    }
}

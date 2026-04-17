package com.cedrickgd.convertx.ui.screens.resize

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cedrickgd.convertx.data.AppContainer
import com.cedrickgd.convertx.domain.AppMode
import com.cedrickgd.convertx.domain.AppView
import com.cedrickgd.convertx.domain.FileType
import com.cedrickgd.convertx.ui.components.AnimatedDropzone
import com.cedrickgd.convertx.ui.components.BigProgressBar
import com.cedrickgd.convertx.ui.components.GradientButton
import com.cedrickgd.convertx.ui.screens.home.FileList
import com.cedrickgd.convertx.ui.screens.home.FilePreview
import com.cedrickgd.convertx.ui.screens.home.HomeViewModel
import com.cedrickgd.convertx.ui.screens.home.HomeViewModelFactory
import com.cedrickgd.convertx.ui.screens.output.OutputPanel
import com.cedrickgd.convertx.domain.FileEntry
import com.cedrickgd.convertx.domain.FileStatus

@Composable
fun ResizeScreen(
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val vm: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(appContainer),
        key = "resize"
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.setMode(AppMode.RESIZE)
    }

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
            val names = uris.map { it.lastPathSegment?.substringAfterLast('/') ?: "image" }
            vm.onFilesPicked(uris, names, context)
        }
    }

    val pickImages: () -> Unit = {
        pickLauncher.launch(arrayOf("image/*"))
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
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "resize-view"
        ) { view ->
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (view) {
                    AppView.IDLE -> AnimatedDropzone(
                        onPickFiles = pickImages,
                        title = "Drop images to resize",
                        subtitle = "PNG · JPG · WebP",
                        buttonLabel = "Pick images"
                    )
                    AppView.READY -> {
                        val images = state.files.filter { it.detectedType == FileType.IMAGE }
                        if (images.size == 1) {
                            FilePreview(entry = images.first())
                        } else {
                            FileList(
                                files = state.files,
                                onRemove = vm::removeFile
                            )
                        }
                        ResizePanel(
                            settings = state.settings,
                            onUpdateSettings = vm::updateSettings,
                            outputTreeUri = state.outputTreeUri,
                            onSetOutputTree = vm::setOutputTree
                        )
                        val imageCount = state.files.count { it.detectedType == FileType.IMAGE }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(onClick = vm::clearAll) {
                                Text("Back")
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            GradientButton(
                                onClick = vm::startResize,
                                enabled = imageCount > 0
                            ) {
                                Text(
                                    text = if (imageCount > 1) "Resize $imageCount images"
                                    else "Resize",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    AppView.CONVERTING -> {
                        FileList(
                            files = state.files,
                            onRemove = vm::removeFile,
                            showRemove = false
                        )
                        BigProgressBar(progress = overallResizeProgress(state.files))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(onClick = vm::cancel) {
                                Text(text = if (state.isCancelling) "Cancelling…" else "Cancel")
                            }
                        }
                    }
                    AppView.DONE -> OutputPanel(
                        files = state.files,
                        onStartOver = vm::clearAll
                    )
                }
            }
        }
    }
}

private fun overallResizeProgress(files: List<FileEntry>): Float {
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

package com.cedrickgd.convertx.ui.screens.home

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cedrickgd.convertx.data.AppContainer
import com.cedrickgd.convertx.data.SettingsRepository
import com.cedrickgd.convertx.domain.AppMode
import com.cedrickgd.convertx.domain.AppSettings
import com.cedrickgd.convertx.domain.AppView
import com.cedrickgd.convertx.domain.ConversionEngine
import com.cedrickgd.convertx.domain.ConversionProgress
import com.cedrickgd.convertx.domain.FileEntry
import com.cedrickgd.convertx.domain.FileStatus
import com.cedrickgd.convertx.domain.FileType
import com.cedrickgd.convertx.domain.Format
import com.cedrickgd.convertx.util.stemOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Snapshot of everything the Home/Resize screens care about.
 */
data class HomeUiState(
    val files: List<FileEntry> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val mode: AppMode = AppMode.CONVERT,
    val view: AppView = AppView.IDLE,
    val outputTreeUri: Uri? = null,
    val isCancelling: Boolean = false,
    val snackbar: String? = null
)

class HomeViewModel(
    private val settingsRepo: SettingsRepository,
    private val engine: ConversionEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepo.outputTreeUri.collect { uri ->
                _uiState.update { it.copy(outputTreeUri = uri) }
            }
        }
    }

    fun onFilesPicked(uris: List<Uri>, displayNames: List<String>, context: Context) {
        if (uris.isEmpty()) return
        val newEntries = uris.mapIndexed { index, uri ->
            val name = displayNames.getOrNull(index) ?: "file_${index + 1}"
            FileEntry(
                id = UUID.randomUUID().toString(),
                sourceUri = uri,
                displayName = name,
                status = FileStatus.DETECTING
            )
        }
        _uiState.update { state ->
            state.copy(
                files = state.files + newEntries,
                view = AppView.READY
            )
        }
        newEntries.forEach { entry ->
            detectEntry(entry, context)
        }
    }

    private fun detectEntry(entry: FileEntry, context: Context) {
        viewModelScope.launch {
            val metadata = runCatching {
                val resolvedName = withContext(Dispatchers.IO) {
                    queryDisplayName(context, entry.sourceUri) ?: entry.displayName
                }
                val detected = engine.detect(entry.sourceUri)
                detected.copy(fileName = resolvedName.ifBlank { detected.fileName })
            }.getOrNull()

            _uiState.update { state ->
                val updated = state.files.map { existing ->
                    if (existing.id != entry.id) existing else {
                        if (metadata == null) {
                            existing.copy(
                                status = FileStatus.ERROR,
                                error = "Could not read file metadata."
                            )
                        } else {
                            existing.copy(
                                metadata = metadata,
                                displayName = metadata.fileName.ifBlank { existing.displayName },
                                outputName = existing.outputName ?: stemOf(
                                    metadata.fileName.ifBlank { existing.displayName }
                                ),
                                status = FileStatus.READY
                            )
                        }
                    }
                }
                state.copy(files = updated)
            }
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        }.getOrNull()
    }

    fun removeFile(id: String) {
        _uiState.update { state ->
            val remaining = state.files.filterNot { it.id == id }
            state.copy(
                files = remaining,
                view = if (remaining.isEmpty()) AppView.IDLE else state.view
            )
        }
    }

    fun clearAll() {
        _uiState.update { state ->
            state.copy(
                files = emptyList(),
                settings = AppSettings(),
                view = AppView.IDLE,
                isCancelling = false
            )
        }
    }

    fun setMode(mode: AppMode) {
        _uiState.update { state ->
            if (state.mode == mode) state else state.copy(
                mode = mode,
                files = emptyList(),
                view = AppView.IDLE,
                settings = AppSettings()
            )
        }
    }

    fun setFormat(format: Format) {
        _uiState.update { state ->
            state.copy(
                settings = state.settings.copy(
                    selectedFormat = format,
                    trimStart = null,
                    trimEnd = null
                )
            )
        }
    }

    fun updateSettings(mutator: (AppSettings) -> AppSettings) {
        _uiState.update { it.copy(settings = mutator(it.settings)) }
    }

    fun setOutputName(fileId: String, name: String) {
        _uiState.update { state ->
            val updated = state.files.map { entry ->
                if (entry.id == fileId) entry.copy(outputName = name) else entry
            }
            state.copy(files = updated)
        }
    }

    fun setOutputTree(uri: Uri?) {
        viewModelScope.launch {
            settingsRepo.setOutputTreeUri(uri)
        }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbar = null) }
    }

    fun startConvert() {
        val snapshot = _uiState.value
        val format = snapshot.settings.selectedFormat ?: run {
            _uiState.update { it.copy(snackbar = "Pick an output format first.") }
            return
        }
        val prepared = snapshot.files.map { entry ->
            val compatible = entry.detectedType in format.accepts
            if (!compatible) {
                entry.copy(
                    status = FileStatus.SKIPPED,
                    error = "Incompatible with ${format.displayName}."
                )
            } else {
                entry.copy(status = FileStatus.QUEUED, error = null, progress = 0f)
            }
        }
        _uiState.update {
            it.copy(files = prepared, view = AppView.CONVERTING, isCancelling = false)
        }
        val options = snapshot.settings.toConvertOptions(format)
        val outputTree = snapshot.outputTreeUri

        viewModelScope.launch {
            val queued = prepared.filter { it.status == FileStatus.QUEUED }
            for (queuedEntry in queued) {
                ensureActive()
                val current = _uiState.value.files.firstOrNull { it.id == queuedEntry.id } ?: continue
                if (_uiState.value.isCancelling) break

                _uiState.update { state ->
                    val updated = state.files.map {
                        if (it.id == queuedEntry.id) it.copy(status = FileStatus.CONVERTING) else it
                    }
                    state.copy(files = updated)
                }

                engine.convert(current, options, outputTree).collect { progress ->
                    ensureActive()
                    applyProgress(queuedEntry.id, progress)
                }
            }
            _uiState.update { it.copy(view = AppView.DONE, isCancelling = false) }
        }
    }

    fun startResize() {
        val snapshot = _uiState.value
        val prepared = snapshot.files.map { entry ->
            if (entry.detectedType != FileType.IMAGE) {
                entry.copy(
                    status = FileStatus.SKIPPED,
                    error = "Resize only supports images."
                )
            } else {
                entry.copy(status = FileStatus.QUEUED, error = null, progress = 0f)
            }
        }
        _uiState.update {
            it.copy(files = prepared, view = AppView.CONVERTING, isCancelling = false)
        }
        val options = snapshot.settings.toResizeOptions()
        val outputTree = snapshot.outputTreeUri

        viewModelScope.launch {
            val queued = prepared.filter { it.status == FileStatus.QUEUED }
            for (queuedEntry in queued) {
                ensureActive()
                val current = _uiState.value.files.firstOrNull { it.id == queuedEntry.id } ?: continue
                if (_uiState.value.isCancelling) break

                _uiState.update { state ->
                    val updated = state.files.map {
                        if (it.id == queuedEntry.id) it.copy(status = FileStatus.CONVERTING) else it
                    }
                    state.copy(files = updated)
                }

                engine.resize(current, options, outputTree).collect { progress ->
                    ensureActive()
                    applyProgress(queuedEntry.id, progress)
                }
            }
            _uiState.update { it.copy(view = AppView.DONE, isCancelling = false) }
        }
    }

    private fun applyProgress(fileId: String, progress: ConversionProgress) {
        _uiState.update { state ->
            val updated = state.files.map { entry ->
                if (entry.id != fileId) return@map entry
                when (progress) {
                    is ConversionProgress.Started -> entry.copy(
                        status = FileStatus.CONVERTING,
                        progress = 0f,
                        elapsed = "00:00",
                        error = null
                    )
                    is ConversionProgress.Tick -> entry.copy(
                        status = FileStatus.CONVERTING,
                        progress = (progress.percent / 100f).coerceIn(0f, 1f),
                        elapsed = progress.elapsed
                    )
                    is ConversionProgress.Completed -> entry.copy(
                        status = FileStatus.DONE,
                        progress = 1f,
                        outputUri = progress.outputUri,
                        outputSize = progress.outputSize
                    )
                    is ConversionProgress.Failed -> entry.copy(
                        status = FileStatus.ERROR,
                        error = progress.error
                    )
                }
            }
            state.copy(files = updated)
        }
    }

    fun cancel() {
        _uiState.update { it.copy(isCancelling = true) }
        engine.cancel()
        _uiState.update { state ->
            val restored = state.files.map { entry ->
                when (entry.status) {
                    FileStatus.QUEUED, FileStatus.CONVERTING -> entry.copy(
                        status = FileStatus.READY,
                        progress = 0f,
                        elapsed = "00:00"
                    )
                    else -> entry
                }
            }
            state.copy(
                files = restored,
                view = AppView.READY,
                isCancelling = false
            )
        }
    }
}

class HomeViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            "Unknown ViewModel: ${modelClass.name}"
        }
        val vm = HomeViewModel(
            settingsRepo = container.settingsRepository,
            engine = container.conversionEngine
        )
        return modelClass.cast(vm)
            ?: throw IllegalArgumentException("Unexpected ViewModel class: ${modelClass.name}")
    }
}

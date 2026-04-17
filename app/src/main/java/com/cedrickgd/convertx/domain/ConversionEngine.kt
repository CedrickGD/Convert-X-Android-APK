package com.cedrickgd.convertx.domain

import android.net.Uri
import kotlinx.coroutines.flow.Flow

sealed interface ConversionProgress {
    data class Started(val totalDuration: Double? = null) : ConversionProgress
    data class Tick(val percent: Float, val elapsed: String) : ConversionProgress
    data class Completed(val outputUri: Uri, val outputSize: Long) : ConversionProgress
    data class Failed(val error: String) : ConversionProgress
}

interface ConversionEngine {
    suspend fun detect(uri: Uri): FileMetadata

    fun convert(
        entry: FileEntry,
        options: ConvertOptions,
        outputTreeUri: Uri?
    ): Flow<ConversionProgress>

    fun resize(
        entry: FileEntry,
        options: ResizeOptions,
        outputTreeUri: Uri?
    ): Flow<ConversionProgress>

    fun cancel()
}

package com.cedrickgd.convertx.domain

import android.net.Uri

enum class FileStatus {
    IDLE, DETECTING, READY, QUEUED, CONVERTING, DONE, ERROR, SKIPPED
}

data class FileMetadata(
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val type: FileType,
    val codec: String? = null,
    val resolution: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val bitrate: String? = null,
    val duration: Double? = null,
    val frameRate: String? = null
)

data class FileEntry(
    val id: String,
    val sourceUri: Uri,
    val displayName: String,
    val metadata: FileMetadata? = null,
    val status: FileStatus = FileStatus.DETECTING,
    val progress: Float = 0f,
    val elapsed: String = "00:00",
    val error: String? = null,
    val outputUri: Uri? = null,
    val outputName: String? = null,
    val outputSize: Long = 0L
) {
    val detectedType: FileType
        get() = metadata?.type ?: FileType.UNKNOWN
}

enum class AppMode { CONVERT, RESIZE }

enum class AppView { IDLE, READY, CONVERTING, DONE }

enum class ResizeMode { PIXELS, PERCENTAGE }

data class ConvertOptions(
    val outputFormat: Format,
    val quality: Int = 85,
    val resolution: String? = null,
    val fps: Int? = null,
    val bitrate: String? = null,
    val preset: String? = null,
    val trimStart: Double? = null,
    val trimEnd: Double? = null,
    val stripAudio: Boolean = false,
    val gifColors: Int? = null,
    val gifDither: String? = null,
    val gifWidth: Int? = null,
    val gifFps: Int? = null,
    val gifTargetSizeMb: Int? = null
)

data class ResizeOptions(
    val mode: ResizeMode,
    val width: Int? = null,
    val height: Int? = null,
    val percentage: Int? = null,
    val keepAspect: Boolean = true,
    val outputFormat: Format? = null,
    val quality: Int = 90
)

data class AppSettings(
    val selectedFormat: Format? = null,
    val quality: Int = 85,
    val resolution: String? = null,
    val fps: Int? = null,
    val bitrate: String? = null,
    val preset: String? = null,
    val trimStart: Double? = null,
    val trimEnd: Double? = null,
    val stripAudio: Boolean = false,
    val gifColors: Int = 256,
    val gifDither: String = "sierra2_4a",
    val gifWidth: Int? = 480,
    val gifFps: Int? = 15,
    val gifTargetSizeMb: Int? = null,
    val resizeMode: ResizeMode = ResizeMode.PERCENTAGE,
    val resizeWidth: Int? = null,
    val resizeHeight: Int? = null,
    val resizePercent: Int = 50,
    val resizeFormat: Format? = null,
    val keepAspect: Boolean = true
) {
    fun toConvertOptions(format: Format): ConvertOptions = ConvertOptions(
        outputFormat = format,
        quality = quality,
        resolution = resolution,
        fps = fps,
        bitrate = bitrate,
        preset = preset,
        trimStart = trimStart,
        trimEnd = trimEnd,
        stripAudio = if (format == Format.GIF) true else stripAudio,
        gifColors = if (format == Format.GIF) gifColors else null,
        gifDither = if (format == Format.GIF) gifDither else null,
        gifWidth = if (format == Format.GIF) gifWidth else null,
        gifFps = if (format == Format.GIF) gifFps else null,
        gifTargetSizeMb = if (format == Format.GIF) gifTargetSizeMb else null
    )

    fun toResizeOptions(): ResizeOptions = ResizeOptions(
        mode = resizeMode,
        width = if (resizeMode == ResizeMode.PIXELS) resizeWidth else null,
        height = if (resizeMode == ResizeMode.PIXELS) resizeHeight else null,
        percentage = if (resizeMode == ResizeMode.PERCENTAGE) resizePercent else null,
        keepAspect = keepAspect,
        outputFormat = resizeFormat,
        quality = quality
    )
}

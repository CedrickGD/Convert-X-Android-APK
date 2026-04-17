package com.cedrickgd.convertx.engine

import com.arthenica.ffmpegkit.MediaInformation
import com.cedrickgd.convertx.domain.FileType
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Aggregate shape used by [FFmpegEngine.detect] to build a [FileMetadata]
 * without littering the engine with null-handling.
 */
internal data class ParsedMediaInfo(
    val durationSeconds: Double? = null,
    val bitrate: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val frameRate: String? = null,
    val detectedType: FileType = FileType.UNKNOWN
) {
    internal val codec: String?
        get() = videoCodec ?: audioCodec

    internal val resolution: String?
        get() = if (width != null && height != null) "${width}x$height" else null
}

/**
 * Parses [MediaInformation] returned by `FFprobeKit.getMediaInformation`.
 * The FFmpegKit API is pure Java / nullable-friendly so we defensively
 * handle every field.
 */
internal object MediaInfoParser {

    internal fun parse(info: MediaInformation?): ParsedMediaInfo {
        if (info == null) return ParsedMediaInfo()

        val duration = info.duration?.toDoubleOrNull()
        val bitrate = info.bitrate?.let { formatBitrateBps(it) }

        val streams = info.streams ?: emptyList()
        var videoCodec: String? = null
        var audioCodec: String? = null
        var width: Int? = null
        var height: Int? = null
        var frameRate: String? = null
        var detectedType = FileType.UNKNOWN

        for (stream in streams) {
            when (stream.type?.lowercase()) {
                "video" -> {
                    if (videoCodec == null) {
                        videoCodec = stream.codec
                        width = stream.width?.toInt()
                        height = stream.height?.toInt()
                        frameRate = parseFrameRate(stream.averageFrameRate ?: stream.realFrameRate)
                        detectedType = FileType.VIDEO
                    }
                }
                "audio" -> {
                    if (audioCodec == null) {
                        audioCodec = stream.codec
                        if (detectedType == FileType.UNKNOWN) {
                            detectedType = FileType.AUDIO
                        }
                    }
                }
                else -> { /* subtitles / data — ignored */ }
            }
        }

        if (detectedType == FileType.VIDEO && duration == null && streams.size == 1) {
            // A single-frame video stream with no duration is commonly an image
            // container (jpg, png, webp seen as image2 by ffprobe).
            detectedType = FileType.IMAGE
        }

        return ParsedMediaInfo(
            durationSeconds = duration,
            bitrate = bitrate,
            width = width,
            height = height,
            videoCodec = videoCodec,
            audioCodec = audioCodec,
            frameRate = frameRate,
            detectedType = detectedType
        )
    }

    /**
     * Expands an FFmpeg avg_frame_rate string like "30000/1001" into "29.97".
     * Returns null for missing, zero, or unparseable inputs.
     */
    internal fun parseFrameRate(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        return if (trimmed.contains('/')) {
            val parts = trimmed.split('/')
            if (parts.size != 2) return null
            val num = parts[0].toDoubleOrNull() ?: return null
            val den = parts[1].toDoubleOrNull() ?: return null
            if (den <= 0.0 || num <= 0.0) null else formatFps(num / den)
        } else {
            trimmed.toDoubleOrNull()?.let { formatFps(it) }
        }
    }

    private fun formatFps(fps: Double): String {
        val rounded = (fps * 100).roundToInt() / 100.0
        return if (rounded == rounded.toInt().toDouble()) {
            rounded.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", rounded)
        }
    }

    /**
     * Formats a raw bits-per-second integer string as `"N kb/s"` or `"N.N Mb/s"`.
     * Falls back to the raw input on parse errors.
     */
    internal fun formatBitrateBps(raw: String): String {
        val value = raw.trim().toLongOrNull() ?: return raw
        if (value <= 0L) return raw
        val kbps = value / 1000.0
        return if (kbps >= 1000.0) {
            String.format(Locale.US, "%.1f Mb/s", kbps / 1000.0)
        } else {
            "${kbps.roundToInt()} kb/s"
        }
    }

}

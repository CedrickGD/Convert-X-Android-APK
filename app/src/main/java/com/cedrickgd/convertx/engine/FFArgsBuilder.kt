package com.cedrickgd.convertx.engine

import com.cedrickgd.convertx.domain.ConvertOptions
import com.cedrickgd.convertx.domain.FileType
import com.cedrickgd.convertx.domain.Format
import com.cedrickgd.convertx.domain.ResizeMode
import com.cedrickgd.convertx.domain.ResizeOptions
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure-Kotlin helpers that transform [ConvertOptions] / [ResizeOptions] into
 * FFmpeg command-line argument lists. No Android dependencies so unit tests
 * can exercise every branch.
 *
 * All functions are package-private by default; call sites are inside the
 * `engine` package.
 */
internal object FFArgsBuilder {

    private const val LOG_LEVEL = "warning"

    /**
     * Standard header flags we prepend to every FFmpeg invocation:
     * overwrite output, hide the banner, and reduce noise in stderr.
     */
    internal fun headerArgs(): List<String> = listOf(
        "-hide_banner",
        "-loglevel", LOG_LEVEL,
        "-y"
    )

    /**
     * Builds the argument list for a single-pass conversion.
     *
     * The GIF two-pass (palette) pipeline is handled separately by
     * [gifPalettePass] and [gifRenderPass] because it needs two FFmpeg
     * sessions sharing an intermediate palette file.
     */
    internal fun buildConvertArgs(
        input: String,
        output: String,
        sourceType: FileType,
        options: ConvertOptions
    ): List<String> {
        val target = options.outputFormat
        val args = mutableListOf<String>()
        args += headerArgs()

        // Image-to-video uses -loop / -t BEFORE -i.
        val isImageToVideo = sourceType == FileType.IMAGE &&
            target.category == FileType.VIDEO && target != Format.GIF
        if (isImageToVideo) {
            args += listOf("-loop", "1")
        }

        // Trim for time-based media applies before -i only when we can seek
        // by keyframes quickly; -ss after -i is safer for accurate trims,
        // so we keep both flags after the input.
        args += listOf("-i", input)

        if (isImageToVideo) {
            args += listOf("-t", DEFAULT_IMAGE_TO_VIDEO_SECONDS.toString())
        }

        if (sourceType == FileType.VIDEO || sourceType == FileType.AUDIO) {
            options.trimStart?.let { args += listOf("-ss", formatSeconds(it)) }
            options.trimEnd?.let { args += listOf("-to", formatSeconds(it)) }
        }

        when (target) {
            Format.JPG -> args += jpgArgs(options)
            Format.PNG -> args += pngArgs()
            Format.WEBP -> args += webpArgs(options)
            Format.BMP -> args += bmpArgs()
            Format.TIFF -> args += tiffArgs()
            Format.ICO -> args += icoArgs(options)
            Format.MP4 -> args += mp4Args(options, sourceType, isImageToVideo)
            Format.MKV -> args += mkvArgs(options, sourceType)
            Format.AVI -> args += aviArgs(options)
            Format.WEBM -> args += webmArgs(options, sourceType, isImageToVideo)
            Format.MOV -> args += movArgs(options, sourceType)
            Format.FLV -> args += flvArgs(options)
            Format.WMV -> args += wmvArgs(options)
            Format.TS -> args += tsArgs(options)
            Format.GIF -> args += gifVideoArgs(options) // video/image → gif single pass
            Format.MP3 -> args += mp3Args(options)
            Format.WAV -> args += wavArgs()
            Format.FLAC -> args += flacArgs()
            Format.OGG -> args += oggArgs(options)
            Format.AAC -> args += aacArgs(options)
            Format.WMA -> args += wmaArgs(options)
            Format.M4A -> args += m4aArgs(options)
            Format.OPUS -> args += opusArgs(options)
        }

        if (options.stripAudio &&
            target.category == FileType.VIDEO && target != Format.GIF
        ) {
            args += "-an"
        }

        args += output
        return args
    }

    /**
     * First GIF pass — generates the palette file.
     */
    internal fun gifPalettePass(
        input: String,
        palettePath: String,
        width: Int?,
        fps: Int?,
        colors: Int
    ): List<String> {
        val args = mutableListOf<String>()
        args += headerArgs()
        args += listOf("-i", input)
        val filter = buildString {
            val effectiveFps = fps ?: DEFAULT_GIF_FPS
            append("fps=").append(effectiveFps)
            append(",scale=").append(width ?: -1).append(":-1:flags=lanczos")
            append(",palettegen=max_colors=").append(colors.coerceIn(2, 256))
        }
        args += listOf("-vf", filter, palettePath)
        return args
    }

    /**
     * Second GIF pass — uses the palette to render the final GIF.
     */
    internal fun gifRenderPass(
        input: String,
        palettePath: String,
        output: String,
        width: Int?,
        fps: Int?,
        dither: String,
        options: ConvertOptions
    ): List<String> {
        val args = mutableListOf<String>()
        args += headerArgs()
        args += listOf("-i", input, "-i", palettePath)
        // Optional trim range for GIF when sourced from a video timeline.
        options.trimStart?.let { args += listOf("-ss", formatSeconds(it)) }
        options.trimEnd?.let { args += listOf("-to", formatSeconds(it)) }
        val filter = gifFilterGraph(width, fps, dither)
        args += listOf("-lavfi", filter, output)
        return args
    }

    /**
     * Filter-graph for paletteuse — intentionally package-internal so tests
     * can assert the exact string we hand to FFmpeg.
     */
    internal fun gifFilterGraph(
        width: Int?,
        fps: Int?,
        dither: String
    ): String {
        val effFps = fps ?: DEFAULT_GIF_FPS
        val effWidth = width ?: -1
        val effDither = effectiveDither(dither)
        return "fps=$effFps,scale=$effWidth:-1:flags=lanczos [x]; " +
            "[x][1:v] paletteuse=dither=$effDither"
    }

    /**
     * Builds the argument list for an image-only resize (JPEG / PNG / WEBP).
     */
    internal fun buildResizeArgs(
        input: String,
        output: String,
        options: ResizeOptions,
        targetFormat: Format
    ): List<String> {
        val args = mutableListOf<String>()
        args += headerArgs()
        args += listOf("-i", input)

        val scaleFilter = when (options.mode) {
            ResizeMode.PIXELS -> pixelScaleFilter(options)
            ResizeMode.PERCENTAGE -> percentScaleFilter(options)
        }
        if (scaleFilter != null) {
            args += listOf("-vf", scaleFilter)
        }

        args += encoderArgsForImage(targetFormat, options.quality)

        args += output
        return args
    }

    // ---- per-format encoder argument builders ----

    private fun jpgArgs(options: ConvertOptions): List<String> {
        val args = mutableListOf("-c:v", "mjpeg")
        val qv = jpegQScale(options.quality)
        args += listOf("-q:v", qv.toString())
        options.resolution?.let { args += listOf("-vf", "scale=$it") }
        return args
    }

    private fun pngArgs(): List<String> = listOf("-c:v", "png")

    private fun webpArgs(options: ConvertOptions): List<String> {
        val args = mutableListOf("-c:v", "libwebp")
        args += listOf("-q:v", webpQuality(options.quality).toString())
        options.resolution?.let { args += listOf("-vf", "scale=$it") }
        return args
    }

    private fun bmpArgs(): List<String> = listOf("-c:v", "bmp")

    private fun tiffArgs(): List<String> = listOf("-c:v", "tiff")

    private fun icoArgs(options: ConvertOptions): List<String> {
        val args = mutableListOf<String>()
        // libwebp cannot produce ICO; use bmp under .ico container path.
        args += listOf("-vf", "scale=${options.resolution ?: "256:256"}")
        return args
    }

    private fun mp4Args(
        options: ConvertOptions,
        sourceType: FileType,
        isImageToVideo: Boolean
    ): List<String> {
        val args = mutableListOf<String>()
        args += listOf("-c:v", "libx264", "-pix_fmt", "yuv420p")
        if (isImageToVideo) {
            args += listOf("-vf", "format=yuv420p")
        } else {
            val vf = videoFilterGraph(options)
            if (vf != null) args += listOf("-vf", vf)
        }
        options.fps?.let { args += listOf("-r", it.toString()) }
        options.bitrate?.let { args += listOf("-b:v", it) }
        options.preset?.let { args += listOf("-preset", it) }
        if (sourceType == FileType.VIDEO && !options.stripAudio) {
            args += listOf("-c:a", "aac", "-b:a", options.bitrate ?: DEFAULT_AUDIO_BITRATE)
        }
        return args
    }

    private fun mkvArgs(options: ConvertOptions, sourceType: FileType): List<String> {
        val args = mutableListOf<String>()
        args += listOf("-c:v", "libx264", "-pix_fmt", "yuv420p")
        val vf = videoFilterGraph(options)
        if (vf != null) args += listOf("-vf", vf)
        options.fps?.let { args += listOf("-r", it.toString()) }
        options.bitrate?.let { args += listOf("-b:v", it) }
        options.preset?.let { args += listOf("-preset", it) }
        if (sourceType == FileType.VIDEO && !options.stripAudio) {
            args += listOf("-c:a", "aac", "-b:a", options.bitrate ?: DEFAULT_AUDIO_BITRATE)
        }
        return args
    }

    private fun aviArgs(options: ConvertOptions): List<String> {
        val args = mutableListOf<String>()
        args += listOf("-c:v", "mpeg4")
        val vf = videoFilterGraph(options)
        if (vf != null) args += listOf("-vf", vf)
        options.fps?.let { args += listOf("-r", it.toString()) }
        options.bitrate?.let { args += listOf("-b:v", it) }
        return args
    }

    private fun webmArgs(
        options: ConvertOptions,
        sourceType: FileType,
        isImageToVideo: Boolean
    ): List<String> {
        val args = mutableListOf<String>()
        args += listOf("-c:v", "libvpx-vp9", "-pix_fmt", "yuv420p")
        if (isImageToVideo) {
            args += listOf("-vf", "format=yuv420p")
        } else {
            val vf = videoFilterGraph(options)
            if (vf != null) args += listOf("-vf", vf)
        }
        options.fps?.let { args += listOf("-r", it.toString()) }
        options.bitrate?.let { args += listOf("-b:v", it) }
        if (sourceType == FileType.VIDEO && !options.stripAudio) {
            args += listOf("-c:a", "libopus", "-b:a", options.bitrate ?: DEFAULT_AUDIO_BITRATE)
        }
        return args
    }

    private fun movArgs(options: ConvertOptions, sourceType: FileType): List<String> {
        val args = mutableListOf<String>()
        args += listOf("-c:v", "libx264", "-pix_fmt", "yuv420p")
        val vf = videoFilterGraph(options)
        if (vf != null) args += listOf("-vf", vf)
        options.fps?.let { args += listOf("-r", it.toString()) }
        options.bitrate?.let { args += listOf("-b:v", it) }
        options.preset?.let { args += listOf("-preset", it) }
        if (sourceType == FileType.VIDEO && !options.stripAudio) {
            args += listOf("-c:a", "aac", "-b:a", options.bitrate ?: DEFAULT_AUDIO_BITRATE)
        }
        return args
    }

    private fun flvArgs(options: ConvertOptions): List<String> {
        val args = mutableListOf<String>()
        args += listOf("-c:v", "flv1")
        val vf = videoFilterGraph(options)
        if (vf != null) args += listOf("-vf", vf)
        options.fps?.let { args += listOf("-r", it.toString()) }
        options.bitrate?.let { args += listOf("-b:v", it) }
        return args
    }

    private fun wmvArgs(options: ConvertOptions): List<String> {
        val args = mutableListOf<String>()
        args += listOf("-c:v", "wmv2")
        val vf = videoFilterGraph(options)
        if (vf != null) args += listOf("-vf", vf)
        options.fps?.let { args += listOf("-r", it.toString()) }
        options.bitrate?.let { args += listOf("-b:v", it) }
        return args
    }

    private fun tsArgs(options: ConvertOptions): List<String> {
        val args = mutableListOf<String>()
        args += listOf("-c:v", "libx264", "-pix_fmt", "yuv420p")
        val vf = videoFilterGraph(options)
        if (vf != null) args += listOf("-vf", vf)
        options.fps?.let { args += listOf("-r", it.toString()) }
        options.bitrate?.let { args += listOf("-b:v", it) }
        options.preset?.let { args += listOf("-preset", it) }
        return args
    }

    /**
     * Single-pass GIF (used when the caller does not want palette optimisation).
     */
    private fun gifVideoArgs(options: ConvertOptions): List<String> {
        val args = mutableListOf<String>()
        val width = options.gifWidth
        val fps = options.gifFps ?: options.fps ?: DEFAULT_GIF_FPS
        val filter = buildString {
            append("fps=").append(fps)
            append(",scale=").append(width ?: -1).append(":-1:flags=lanczos")
        }
        args += listOf("-vf", filter)
        return args
    }

    // Audio encoders

    private fun mp3Args(options: ConvertOptions): List<String> =
        listOf("-c:a", "libmp3lame", "-b:a", options.bitrate ?: DEFAULT_AUDIO_BITRATE, "-vn")

    private fun wavArgs(): List<String> =
        listOf("-c:a", "pcm_s16le", "-vn")

    private fun flacArgs(): List<String> =
        listOf("-c:a", "flac", "-vn")

    private fun oggArgs(options: ConvertOptions): List<String> =
        listOf("-c:a", "libvorbis", "-b:a", options.bitrate ?: DEFAULT_AUDIO_BITRATE, "-vn")

    private fun aacArgs(options: ConvertOptions): List<String> =
        listOf("-c:a", "aac", "-b:a", options.bitrate ?: DEFAULT_AUDIO_BITRATE, "-vn")

    private fun wmaArgs(options: ConvertOptions): List<String> =
        listOf("-c:a", "wmav2", "-b:a", options.bitrate ?: DEFAULT_AUDIO_BITRATE, "-vn")

    private fun m4aArgs(options: ConvertOptions): List<String> =
        listOf("-c:a", "aac", "-b:a", options.bitrate ?: DEFAULT_AUDIO_BITRATE, "-vn")

    private fun opusArgs(options: ConvertOptions): List<String> =
        listOf("-c:a", "libopus", "-b:a", options.bitrate ?: DEFAULT_AUDIO_BITRATE, "-vn")

    private fun encoderArgsForImage(format: Format, quality: Int): List<String> = when (format) {
        Format.JPG -> listOf("-c:v", "mjpeg", "-q:v", jpegQScale(quality).toString())
        Format.PNG -> listOf("-c:v", "png")
        Format.WEBP -> listOf("-c:v", "libwebp", "-q:v", webpQuality(quality).toString())
        Format.BMP -> listOf("-c:v", "bmp")
        Format.TIFF -> listOf("-c:v", "tiff")
        else -> emptyList()
    }

    private fun videoFilterGraph(options: ConvertOptions): String? {
        val parts = mutableListOf<String>()
        options.resolution?.let { parts += "scale=$it" }
        return if (parts.isEmpty()) null else parts.joinToString(",")
    }

    private fun pixelScaleFilter(options: ResizeOptions): String? {
        val w = options.width
        val h = options.height
        if (w == null && h == null) return null
        val ww = w ?: -1
        val hh = h ?: -1
        return if (options.keepAspect) {
            "scale=$ww:$hh:force_original_aspect_ratio=decrease"
        } else {
            "scale=$ww:$hh"
        }
    }

    private fun percentScaleFilter(options: ResizeOptions): String? {
        val pct = options.percentage ?: return null
        return "scale=iw*$pct/100:ih*$pct/100"
    }

    // ---- quality mappings ----

    /**
     * Maps a 0..100 "quality" slider to the MJPEG qscale range 2..31 where
     * lower numbers are higher quality.
     */
    internal fun jpegQScale(quality: Int): Int {
        val clamped = quality.coerceIn(0, 100)
        val raw = 31.0 - clamped * 0.29
        return max(2, min(31, raw.roundToInt()))
    }

    /**
     * libwebp uses 0..100 directly.
     */
    internal fun webpQuality(quality: Int): Int = quality.coerceIn(0, 100)

    /**
     * Validates a dither name for paletteuse, falling back to Sierra-2-4A.
     */
    internal fun effectiveDither(requested: String?): String {
        if (requested.isNullOrBlank()) return DEFAULT_DITHER
        val normalized = requested.lowercase(Locale.US)
        return if (normalized in ALLOWED_DITHERS) normalized else DEFAULT_DITHER
    }

    /**
     * Renders a double number of seconds as an HH:MM:SS.mmm string that
     * FFmpeg understands without locale surprises.
     */
    internal fun formatSeconds(seconds: Double): String {
        val total = max(0.0, seconds)
        val hours = (total / 3600).toInt()
        val minutes = ((total % 3600) / 60).toInt()
        val secs = total - hours * 3600 - minutes * 60
        return String.format(Locale.US, "%02d:%02d:%06.3f", hours, minutes, secs)
    }

    // ---- constants ----

    internal const val DEFAULT_GIF_FPS: Int = 15
    internal const val DEFAULT_AUDIO_BITRATE: String = "192k"
    internal const val DEFAULT_DITHER: String = "sierra2_4a"
    internal const val DEFAULT_IMAGE_TO_VIDEO_SECONDS: Int = 3

    private val ALLOWED_DITHERS: Set<String> = setOf(
        "none", "bayer", "heckbert", "floyd_steinberg",
        "sierra2", "sierra2_4a"
    )
}

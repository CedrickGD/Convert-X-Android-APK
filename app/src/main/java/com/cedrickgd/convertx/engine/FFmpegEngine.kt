package com.cedrickgd.convertx.engine

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFprobeKit
import com.cedrickgd.convertx.domain.ConversionEngine
import com.cedrickgd.convertx.domain.ConversionProgress
import com.cedrickgd.convertx.domain.ConvertOptions
import com.cedrickgd.convertx.domain.FileEntry
import com.cedrickgd.convertx.domain.FileMetadata
import com.cedrickgd.convertx.domain.FileType
import com.cedrickgd.convertx.domain.Format
import com.cedrickgd.convertx.domain.ResizeOptions
import com.cedrickgd.convertx.util.extensionOf
import com.cedrickgd.convertx.util.stemOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * FFmpegKit-backed implementation of [ConversionEngine].
 *
 * The class holds a single "active session" reference so [cancel] can abort
 * any in-flight FFmpeg invocation. Progress is surfaced through
 * [FFmpegKitConfig.enableStatisticsCallback]; each Flow registers its own
 * callback and unregisters on close to avoid leaking when the UI stops
 * collecting.
 */
class FFmpegEngine(private val context: Context) : ConversionEngine {

    private val activeSessionId: AtomicLong = AtomicLong(SESSION_NONE)

    @Volatile
    private var cancelRequested: Boolean = false

    // ---- detect ----

    override suspend fun detect(uri: Uri): FileMetadata = withContext(Dispatchers.IO) {
        val displayName = SafIo.queryDisplayName(context, uri)
            ?: uri.lastPathSegment
            ?: "unknown"

        val size = SafIo.queryFileSize(context, uri)

        val mime = context.contentResolver.getType(uri)
            ?: Format.fromExtension(extensionOf(displayName))?.mime
            ?: "application/octet-stream"

        val safInput = SafIo.resolveInput(context, uri)
        val probe = runCatching { FFprobeKit.getMediaInformation(safInput) }.getOrNull()
        val parsed = MediaInfoParser.parse(probe?.mediaInformation)

        val mimeType = Format.typeFromMime(mime)
        val effectiveType = when {
            mimeType != FileType.UNKNOWN -> mimeType
            parsed.detectedType != FileType.UNKNOWN -> parsed.detectedType
            else -> FileType.UNKNOWN
        }

        val duration = if (effectiveType == FileType.IMAGE) null else parsed.durationSeconds

        FileMetadata(
            fileName = displayName,
            mimeType = mime,
            size = size,
            type = effectiveType,
            codec = parsed.codec,
            resolution = parsed.resolution,
            width = parsed.width,
            height = parsed.height,
            bitrate = parsed.bitrate,
            duration = duration,
            frameRate = parsed.frameRate
        )
    }

    // ---- convert ----

    override fun convert(
        entry: FileEntry,
        options: ConvertOptions,
        outputTreeUri: Uri?
    ): Flow<ConversionProgress> = callbackFlow {
        cancelRequested = false
        val sourceType = entry.metadata?.type ?: FileType.UNKNOWN
        val target = options.outputFormat
        val emit: (ConversionProgress) -> Unit = { progress -> trySendBlocking(progress) }

        if (!Format.isCompatible(sourceType, target) && sourceType != FileType.UNKNOWN) {
            emit(
                ConversionProgress.Failed(
                    "Unsupported: cannot convert ${sourceType.name.lowercase(Locale.US)} " +
                        "to ${target.displayName}"
                )
            )
            close()
            awaitClose { }
            return@callbackFlow
        }

        val outputFileName = FileNaming.defaultOutputName(entry, options)
        val destination = resolveDestination(outputTreeUri, outputFileName, target.mime)
        if (destination == null) {
            emit(ConversionProgress.Failed("Failed to create output file"))
            close()
            awaitClose { }
            return@callbackFlow
        }

        val inputPath = SafIo.resolveInput(context, entry.sourceUri)
        val totalMs = totalMillis(entry, options)
        emit(ConversionProgress.Started(totalDuration = entry.metadata?.duration))

        val ok = if (target == Format.GIF && sourceType == FileType.VIDEO) {
            runGifTwoPass(inputPath, destination, options, totalMs, emit)
        } else {
            runSinglePass(inputPath, destination, sourceType, options, totalMs, emit)
        }

        if (!ok) {
            runCatching { destination.localFile?.delete() }
        }
        close()
        awaitClose {
            FFmpegKitConfig.enableStatisticsCallback(null)
            val id = activeSessionId.getAndSet(SESSION_NONE)
            if (id != SESSION_NONE) runCatching { FFmpegKit.cancel(id) }
        }
    }.flowOn(Dispatchers.IO)

    // ---- resize ----

    override fun resize(
        entry: FileEntry,
        options: ResizeOptions,
        outputTreeUri: Uri?
    ): Flow<ConversionProgress> = callbackFlow {
        cancelRequested = false
        val sourceType = entry.metadata?.type ?: FileType.UNKNOWN
        val emit: (ConversionProgress) -> Unit = { progress -> trySendBlocking(progress) }

        if (sourceType != FileType.IMAGE) {
            emit(ConversionProgress.Failed("Unsupported: resize requires an image source"))
            close()
            awaitClose { }
            return@callbackFlow
        }

        val inferredFormat = options.outputFormat
            ?: Format.fromExtension(extensionOf(entry.displayName))
            ?: Format.JPG
        if (inferredFormat.category != FileType.IMAGE) {
            emit(ConversionProgress.Failed("Unsupported: resize output must be an image"))
            close()
            awaitClose { }
            return@callbackFlow
        }

        val stem = entry.outputName?.takeIf { it.isNotBlank() }
            ?: stemOf(entry.displayName).ifBlank { "output" }
        val outputFileName = "$stem.${inferredFormat.extension}"
        val destination = resolveDestination(outputTreeUri, outputFileName, inferredFormat.mime)
        if (destination == null) {
            emit(ConversionProgress.Failed("Failed to create output file"))
            close()
            awaitClose { }
            return@callbackFlow
        }

        val inputPath = SafIo.resolveInput(context, entry.sourceUri)
        emit(ConversionProgress.Started(totalDuration = null))

        val args = FFArgsBuilder.buildResizeArgs(
            input = inputPath,
            output = destination.ffmpegTarget,
            options = options,
            targetFormat = inferredFormat
        )

        val ok = executeFfmpeg(args = args, totalMs = 0L, destination = destination, emit = emit)
        if (!ok) {
            runCatching { destination.localFile?.delete() }
        }
        close()
        awaitClose {
            FFmpegKitConfig.enableStatisticsCallback(null)
            val id = activeSessionId.getAndSet(SESSION_NONE)
            if (id != SESSION_NONE) runCatching { FFmpegKit.cancel(id) }
        }
    }.flowOn(Dispatchers.IO)

    // ---- cancel ----

    override fun cancel() {
        cancelRequested = true
        val id = activeSessionId.getAndSet(SESSION_NONE)
        if (id != SESSION_NONE) {
            runCatching { FFmpegKit.cancel(id) }
        } else {
            runCatching { FFmpegKit.cancel() }
        }
    }

    // ---- helpers ----

    /**
     * Picks where the output file lives. If [treeUri] is non-null we create
     * a DocumentFile under it; otherwise we write to the app's external
     * files dir and hand the UI a FileProvider URI for sharing.
     */
    private fun resolveDestination(
        treeUri: Uri?,
        fileName: String,
        mime: String
    ): Destination? {
        if (treeUri != null) {
            val parentDir = SafIo.asDirectory(context, treeUri) ?: return null
            val baseName = stemOf(fileName).ifBlank { "output" }
            val ext = extensionOf(fileName).ifBlank { "bin" }
            val safeName = FileNaming.uniqueDisplayName(parentDir, baseName, ext)
            val childUri = parentDir.createFile(mime, safeName)?.uri ?: return null
            val ffmpegPath = SafIo.childUriToSafOutput(context, childUri)
            return Destination(
                outputUri = childUri,
                ffmpegTarget = ffmpegPath,
                localFile = null
            )
        }

        val outDir = File(context.getExternalFilesDir(null), DEFAULT_OUTPUT_DIR)
        if (!outDir.exists()) outDir.mkdirs()
        val safeName = nextAvailableFile(outDir, fileName)
        val target = File(outDir, safeName)
        val providerUri = FileProvider.getUriForFile(
            context,
            context.packageName + FILEPROVIDER_SUFFIX,
            target
        )
        return Destination(
            outputUri = providerUri,
            ffmpegTarget = target.absolutePath,
            localFile = target
        )
    }

    /**
     * Runs a single FFmpeg session for a conversion.
     * Returns true on success.
     */
    private fun runSinglePass(
        input: String,
        destination: Destination,
        sourceType: FileType,
        options: ConvertOptions,
        totalMs: Long,
        emit: (ConversionProgress) -> Unit
    ): Boolean {
        val args = FFArgsBuilder.buildConvertArgs(
            input = input,
            output = destination.ffmpegTarget,
            sourceType = sourceType,
            options = options
        )
        return executeFfmpeg(args, totalMs, destination, emit)
    }

    /**
     * Two-pass palette GIF — generates a palette in the cache dir, then
     * renders the final GIF using it. Retries with reduced quality a handful
     * of times when [ConvertOptions.gifTargetSizeMb] is supplied.
     */
    private fun runGifTwoPass(
        input: String,
        destination: Destination,
        options: ConvertOptions,
        totalMs: Long,
        emit: (ConversionProgress) -> Unit
    ): Boolean {
        var width = options.gifWidth
        var fps = options.gifFps
        var colors = options.gifColors ?: DEFAULT_GIF_COLORS
        val dither = options.gifDither ?: FFArgsBuilder.DEFAULT_DITHER
        val targetBytes = options.gifTargetSizeMb?.let { it.toLong() * BYTES_PER_MEBIBYTE }

        val cacheDir = File(context.cacheDir, GIF_CACHE_SUBDIR).apply { mkdirs() }
        val palette = File(cacheDir, "palette-${System.currentTimeMillis()}.png")

        var attempt = 0
        var success: Boolean
        while (true) {
            if (cancelRequested) {
                emit(ConversionProgress.Failed("Cancelled"))
                return false
            }
            val paletteArgs = FFArgsBuilder.gifPalettePass(
                input = input,
                palettePath = palette.absolutePath,
                width = width,
                fps = fps,
                colors = colors
            )
            if (!executeFfmpeg(paletteArgs, 0L, null, emit)) {
                runCatching { palette.delete() }
                return false
            }

            val renderArgs = FFArgsBuilder.gifRenderPass(
                input = input,
                palettePath = palette.absolutePath,
                output = destination.ffmpegTarget,
                width = width,
                fps = fps,
                dither = dither,
                options = options
            )
            success = executeFfmpeg(renderArgs, totalMs, destination, emit, suppressCompleted = true)
            if (!success) {
                runCatching { palette.delete() }
                return false
            }

            val outputSize = destination.currentSize(context)
            if (targetBytes == null || outputSize <= targetBytes || attempt >= MAX_GIF_RETRIES) {
                runCatching { palette.delete() }
                emit(ConversionProgress.Completed(destination.outputUri, outputSize))
                return true
            }
            // Shrink aggressiveness for next attempt.
            colors = max(16, (colors * GIF_RETRY_FACTOR).roundToInt())
            fps = ((fps ?: FFArgsBuilder.DEFAULT_GIF_FPS) * GIF_RETRY_FACTOR).roundToInt()
                .coerceAtLeast(MIN_GIF_FPS)
            width = width?.let { ((it * GIF_RETRY_FACTOR).roundToInt()).coerceAtLeast(MIN_GIF_WIDTH) }
                ?: DEFAULT_GIF_WIDTH
            attempt++
        }
    }

    /**
     * Drives a single FFmpegKit session and forwards progress via [emit].
     *
     * When [suppressCompleted] is true the caller is responsible for emitting
     * the Completed event (used by the GIF retry loop).
     */
    private fun executeFfmpeg(
        args: List<String>,
        totalMs: Long,
        destination: Destination?,
        emit: (ConversionProgress) -> Unit,
        suppressCompleted: Boolean = false
    ): Boolean {
        val lock = java.util.concurrent.CountDownLatch(1)
        val result = AtomicReference<FFmpegSession?>(null)

        FFmpegKitConfig.enableStatisticsCallback { stats ->
            val expected = activeSessionId.get()
            // Global callback — filter to the session we are tracking.
            if (expected != SESSION_NONE && stats.sessionId != expected) {
                return@enableStatisticsCallback
            }
            val elapsedMs = stats.time.toLong()
            val percent = if (totalMs <= 0L) {
                0f
            } else {
                ((elapsedMs.toDouble() / totalMs.toDouble()) * 100.0)
                    .coerceIn(0.0, 100.0)
                    .toFloat()
            }
            emit(ConversionProgress.Tick(percent = percent, elapsed = formatElapsed(elapsedMs)))
        }

        val session = FFmpegKit.executeWithArgumentsAsync(args.toTypedArray()) { finished ->
            result.set(finished)
            lock.countDown()
        }
        activeSessionId.set(session.sessionId)
        try {
            lock.await()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            emit(ConversionProgress.Failed("Interrupted"))
            activeSessionId.compareAndSet(session.sessionId, SESSION_NONE)
            return false
        }

        activeSessionId.compareAndSet(session.sessionId, SESSION_NONE)

        val completed = result.get() ?: return false
        val rc = completed.returnCode
        return when {
            rc != null && rc.isValueSuccess -> {
                if (!suppressCompleted && destination != null) {
                    val size = destination.currentSize(context)
                    emit(ConversionProgress.Completed(destination.outputUri, size))
                }
                true
            }
            rc != null && rc.isValueCancel -> {
                emit(ConversionProgress.Failed("Cancelled"))
                false
            }
            else -> {
                val stack = completed.failStackTrace
                val message = if (!stack.isNullOrBlank()) stack else "Exit ${rc?.value ?: -1}"
                emit(ConversionProgress.Failed(message))
                false
            }
        }
    }

    private fun totalMillis(entry: FileEntry, options: ConvertOptions): Long {
        val total = entry.metadata?.duration ?: return 0L
        val start = options.trimStart ?: 0.0
        val end = options.trimEnd ?: total
        val span = max(0.0, min(total, end) - max(0.0, start))
        return (span * 1000.0).toLong()
    }

    private fun formatElapsed(millis: Long): String {
        val totalSec = max(0L, millis / 1000L)
        val minutes = totalSec / 60L
        val seconds = totalSec % 60L
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    private fun nextAvailableFile(dir: File, desired: String): String {
        val stem = stemOf(desired).ifBlank { "output" }
        val ext = extensionOf(desired).ifBlank { "bin" }
        if (!File(dir, desired).exists()) return desired
        var i = 1
        while (i < MAX_COLLISION_ATTEMPTS) {
            val candidate = "$stem-$i.$ext"
            if (!File(dir, candidate).exists()) return candidate
            i++
        }
        return desired
    }

    /**
     * Bundle that captures everything we need about a conversion output:
     * the URI we give back to the UI, the path/URL FFmpeg writes to, and
     * the local [File] (only set when writing to our own cache area) used
     * to query size + delete partial outputs.
     */
    private data class Destination(
        val outputUri: Uri,
        val ffmpegTarget: String,
        val localFile: File?
    ) {
        fun currentSize(ctx: Context): Long {
            localFile?.let { if (it.exists()) return it.length() }
            return if (outputUri.scheme == ContentResolver.SCHEME_CONTENT) {
                SafIo.queryFileSize(ctx, outputUri)
            } else {
                0L
            }
        }
    }

    companion object {
        private const val SESSION_NONE: Long = 0L
        private const val DEFAULT_OUTPUT_DIR: String = "Convert-X"
        private const val FILEPROVIDER_SUFFIX: String = ".fileprovider"
        private const val GIF_CACHE_SUBDIR: String = "gif-palette"
        private const val DEFAULT_GIF_COLORS: Int = 256
        private const val DEFAULT_GIF_WIDTH: Int = 480
        private const val MIN_GIF_WIDTH: Int = 160
        private const val MIN_GIF_FPS: Int = 5
        private const val MAX_GIF_RETRIES: Int = 3
        private const val GIF_RETRY_FACTOR: Double = 0.75
        private const val BYTES_PER_MEBIBYTE: Long = 1024L * 1024L
        private const val MAX_COLLISION_ATTEMPTS: Int = 1_000
    }
}

package com.cedrickgd.convertx.engine

import androidx.documentfile.provider.DocumentFile
import com.cedrickgd.convertx.domain.ConvertOptions
import com.cedrickgd.convertx.domain.FileEntry
import com.cedrickgd.convertx.util.stemOf

/**
 * Naming helpers used by [FFmpegEngine] when producing output files.
 *
 * None of these touch FFmpeg directly so they are easy to cover with unit
 * tests and keep the engine code readable.
 */
internal object FileNaming {

    /**
     * Derives the user-visible output file name (without path) for a given
     * conversion. Honors [FileEntry.outputName] when supplied, otherwise
     * reuses the input's stem with the target extension.
     */
    internal fun defaultOutputName(entry: FileEntry, options: ConvertOptions): String {
        val stem = entry.outputName?.takeIf { it.isNotBlank() }
            ?: stemOf(entry.displayName).ifBlank { "output" }
        val ext = options.outputFormat.extension
        return "$stem.$ext"
    }

    /**
     * Picks a file name that does not collide with existing children under
     * [parent]. Appends `-1`, `-2` … until a free slot is found. Falls back
     * to the naive name after a hard cap so we never spin forever.
     */
    internal fun uniqueDisplayName(
        parent: DocumentFile?,
        baseName: String,
        ext: String
    ): String {
        val desired = "$baseName.$ext"
        if (parent == null || !parent.isDirectory) return desired
        if (parent.findFile(desired) == null) return desired
        var i = 1
        while (i < MAX_COLLISION_ATTEMPTS) {
            val candidate = "$baseName-$i.$ext"
            if (parent.findFile(candidate) == null) return candidate
            i++
        }
        return desired
    }

    private const val MAX_COLLISION_ATTEMPTS: Int = 1_000
}

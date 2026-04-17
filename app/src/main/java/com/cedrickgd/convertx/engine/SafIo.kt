package com.cedrickgd.convertx.engine

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.arthenica.ffmpegkit.FFmpegKitConfig

/**
 * Thin wrappers around SAF / DocumentFile / ContentResolver so the engine
 * only deals with plain strings and Uris.
 */
internal object SafIo {

    /**
     * Queries OpenableColumns.DISPLAY_NAME for the given content URI.
     * Returns null when the provider does not expose a name or the URI is
     * not a content:// scheme.
     */
    internal fun queryDisplayName(ctx: Context, uri: Uri): String? {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return uri.lastPathSegment
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        ctx.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && !cursor.isNull(idx)) return cursor.getString(idx)
            }
        }
        return null
    }

    /**
     * Queries OpenableColumns.SIZE for the given content URI. Returns 0
     * when the provider does not report a size.
     */
    internal fun queryFileSize(ctx: Context, uri: Uri): Long {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return 0L
        val projection = arrayOf(OpenableColumns.SIZE)
        ctx.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0 && !cursor.isNull(idx)) return cursor.getLong(idx)
            }
        }
        return 0L
    }

    /**
     * Resolves an input URI into a string FFmpeg can consume. For content://
     * URIs this is a `saf:…` URL produced by FFmpegKit; for file:// URIs
     * it's the raw path.
     */
    internal fun resolveInput(ctx: Context, uri: Uri): String = when (uri.scheme) {
        ContentResolver.SCHEME_FILE -> uri.path ?: uri.toString()
        ContentResolver.SCHEME_CONTENT ->
            FFmpegKitConfig.getSafParameterForRead(ctx, uri) ?: uri.toString()
        else -> uri.toString()
    }

    /**
     * Wraps a destination URI so FFmpeg can write into it via SAF.
     */
    internal fun childUriToSafOutput(ctx: Context, uri: Uri): String =
        FFmpegKitConfig.getSafParameterForWrite(ctx, uri) ?: uri.toString()

    /**
     * Creates a new child file under [treeUri]. Returns null when the tree
     * URI cannot be resolved into a directory.
     */
    internal fun createChild(
        ctx: Context,
        treeUri: Uri,
        name: String,
        mime: String
    ): Uri? {
        val dir = DocumentFile.fromTreeUri(ctx, treeUri) ?: return null
        if (!dir.isDirectory) return null
        val safeName = FileNaming.uniqueDisplayName(
            parent = dir,
            baseName = stemBeforeExt(name),
            ext = extensionOrBlank(name)
        )
        val created = dir.createFile(mime, safeName) ?: return null
        return created.uri
    }

    /**
     * Exposes the [DocumentFile] for a tree URI — used by the engine when it
     * needs to call [FileNaming.uniqueDisplayName] before creation.
     */
    internal fun asDirectory(ctx: Context, treeUri: Uri): DocumentFile? =
        DocumentFile.fromTreeUri(ctx, treeUri)?.takeIf { it.isDirectory }

    private fun stemBeforeExt(name: String): String {
        val idx = name.lastIndexOf('.')
        return if (idx > 0) name.substring(0, idx) else name
    }

    private fun extensionOrBlank(name: String): String {
        val idx = name.lastIndexOf('.')
        return if (idx in 0 until name.length - 1) name.substring(idx + 1) else ""
    }
}

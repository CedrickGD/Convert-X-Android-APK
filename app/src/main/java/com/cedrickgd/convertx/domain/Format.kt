package com.cedrickgd.convertx.domain

enum class FileType { IMAGE, VIDEO, AUDIO, UNKNOWN }

/**
 * The catalog of formats Convert-X supports, mirroring the desktop app's matrix.
 * Each format declares which input types can produce it.
 */
enum class Format(
    val extension: String,
    val displayName: String,
    val mime: String,
    val accepts: Set<FileType>
) {
    // Video
    MP4("mp4", "MP4", "video/mp4", setOf(FileType.VIDEO, FileType.IMAGE)),
    MKV("mkv", "MKV", "video/x-matroska", setOf(FileType.VIDEO)),
    AVI("avi", "AVI", "video/x-msvideo", setOf(FileType.VIDEO)),
    WEBM("webm", "WebM", "video/webm", setOf(FileType.VIDEO, FileType.IMAGE)),
    MOV("mov", "MOV", "video/quicktime", setOf(FileType.VIDEO)),
    FLV("flv", "FLV", "video/x-flv", setOf(FileType.VIDEO)),
    WMV("wmv", "WMV", "video/x-ms-wmv", setOf(FileType.VIDEO)),
    TS("ts", "TS", "video/mp2t", setOf(FileType.VIDEO)),
    GIF("gif", "GIF", "image/gif", setOf(FileType.VIDEO, FileType.IMAGE)),

    // Image
    PNG("png", "PNG", "image/png", setOf(FileType.IMAGE, FileType.VIDEO)),
    JPG("jpg", "JPG", "image/jpeg", setOf(FileType.IMAGE, FileType.VIDEO)),
    WEBP("webp", "WebP", "image/webp", setOf(FileType.IMAGE, FileType.VIDEO)),
    BMP("bmp", "BMP", "image/bmp", setOf(FileType.IMAGE)),
    TIFF("tiff", "TIFF", "image/tiff", setOf(FileType.IMAGE)),
    ICO("ico", "ICO", "image/x-icon", setOf(FileType.IMAGE)),

    // Audio
    MP3("mp3", "MP3", "audio/mpeg", setOf(FileType.AUDIO, FileType.VIDEO)),
    WAV("wav", "WAV", "audio/wav", setOf(FileType.AUDIO, FileType.VIDEO)),
    FLAC("flac", "FLAC", "audio/flac", setOf(FileType.AUDIO, FileType.VIDEO)),
    OGG("ogg", "OGG", "audio/ogg", setOf(FileType.AUDIO, FileType.VIDEO)),
    AAC("aac", "AAC", "audio/aac", setOf(FileType.AUDIO, FileType.VIDEO)),
    WMA("wma", "WMA", "audio/x-ms-wma", setOf(FileType.AUDIO, FileType.VIDEO)),
    M4A("m4a", "M4A", "audio/mp4", setOf(FileType.AUDIO, FileType.VIDEO)),
    OPUS("opus", "Opus", "audio/opus", setOf(FileType.AUDIO, FileType.VIDEO));

    val category: FileType
        get() = when (this) {
            MP4, MKV, AVI, WEBM, MOV, FLV, WMV, TS -> FileType.VIDEO
            GIF, PNG, JPG, WEBP, BMP, TIFF, ICO -> FileType.IMAGE
            MP3, WAV, FLAC, OGG, AAC, WMA, M4A, OPUS -> FileType.AUDIO
        }

    companion object {
        fun fromExtension(ext: String): Format? = entries.firstOrNull {
            it.extension.equals(ext, ignoreCase = true)
        }

        fun fromMime(mime: String): Format? = entries.firstOrNull {
            it.mime.equals(mime, ignoreCase = true)
        }

        fun typeFromMime(mime: String?): FileType = when {
            mime == null -> FileType.UNKNOWN
            mime.startsWith("image/") -> FileType.IMAGE
            mime.startsWith("video/") -> FileType.VIDEO
            mime.startsWith("audio/") -> FileType.AUDIO
            else -> FileType.UNKNOWN
        }

        fun isCompatible(sourceType: FileType, target: Format): Boolean =
            sourceType in target.accepts
    }
}

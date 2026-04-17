package com.cedrickgd.convertx.util

import java.util.Locale
import kotlin.math.max

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024.0 && i < units.lastIndex) {
        value /= 1024.0
        i++
    }
    return if (i == 0) {
        String.format(Locale.US, "%d %s", bytes, units[i])
    } else {
        String.format(Locale.US, "%.1f %s", value, units[i])
    }
}

fun formatDuration(seconds: Double): String {
    val total = max(0, seconds.toInt())
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%d:%02d", m, s)
    }
}

fun stemOf(name: String): String {
    val idx = name.lastIndexOf('.')
    return if (idx > 0) name.substring(0, idx) else name
}

fun extensionOf(name: String): String {
    val idx = name.lastIndexOf('.')
    return if (idx >= 0 && idx < name.length - 1) name.substring(idx + 1).lowercase() else ""
}

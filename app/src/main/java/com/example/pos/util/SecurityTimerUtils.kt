package com.example.pos.util

object SecurityTimerUtils {

    val AUTO_LOCK_PRESETS = listOf(
        "30 seconds",
        "1 minute",
        "2 minutes",
        "5 minutes",
        "10 minutes",
        "15 minutes",
        "30 minutes",
        "Never"
    )

    val AUTO_LOGOUT_PRESETS = listOf(
        "Never",
        "15 minutes",
        "30 minutes",
        "1 hour",
        "2 hours",
        "4 hours",
        "8 hours"
    )

    fun parseIntervalToMillis(interval: String): Long {
        val clean = interval.trim().lowercase()
        if (clean.isEmpty() || clean == "never" || clean == "off" || clean == "disabled") {
            return 0L
        }
        val secMatch = Regex("""(\d+)\s*(?:sec|second|seconds|s)""").find(clean)
        if (secMatch != null) {
            return (secMatch.groupValues[1].toLongOrNull() ?: 0L) * 1000L
        }
        val hrMatch = Regex("""(\d+)\s*(?:hour|hours|hr|hrs|h)""").find(clean)
        if (hrMatch != null) {
            return (hrMatch.groupValues[1].toLongOrNull() ?: 0L) * 3600_000L
        }
        val minMatch = Regex("""(\d+)\s*(?:min|minute|minutes|m)""").find(clean)
        if (minMatch != null) {
            return (minMatch.groupValues[1].toLongOrNull() ?: 0L) * 60_000L
        }
        val plainNum = Regex("""\d+""").find(clean)?.value?.toLongOrNull()
        if (plainNum != null) {
            return plainNum * 60_000L
        }
        return 0L
    }

    fun formatDuration(millis: Long): String {
        if (millis <= 0L) return "Disabled"
        val seconds = millis / 1000L
        if (seconds < 60) return "$seconds sec"
        val minutes = seconds / 60
        if (minutes < 60) return "$minutes min"
        val hours = minutes / 60
        val remainingMin = minutes % 60
        return if (remainingMin > 0) "${hours}h ${remainingMin}m" else "${hours}h"
    }
}

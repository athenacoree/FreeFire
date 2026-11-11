package com.athenacoree.music

/** A timestamped line imported from a user-owned LRC file. */
internal data class MusicLyricsLine(val timeMs: Long, val text: String)

internal object MusicLyricsParser {
    private val timestamp = Regex("""\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]""")

    fun parse(source: String): List<MusicLyricsLine> {
        val result = mutableListOf<MusicLyricsLine>()
        source.lineSequence().forEach { raw ->
            val matches = timestamp.findAll(raw).toList()
            if (matches.isEmpty()) return@forEach
            val lyric = timestamp.replace(raw, "").trim()
            if (lyric.isBlank()) return@forEach
            matches.forEach { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: return@forEach
                val seconds = match.groupValues[2].toLongOrNull() ?: return@forEach
                val fractionText = match.groupValues[3]
                val millis = when (fractionText.length) {
                    1 -> (fractionText.toLongOrNull() ?: 0L) * 100L
                    2 -> (fractionText.toLongOrNull() ?: 0L) * 10L
                    3 -> fractionText.toLongOrNull() ?: 0L
                    else -> 0L
                }
                if (seconds < 60) result += MusicLyricsLine((minutes * 60 + seconds) * 1000 + millis, lyric)
            }
        }
        return result.sortedBy { it.timeMs }
    }
}

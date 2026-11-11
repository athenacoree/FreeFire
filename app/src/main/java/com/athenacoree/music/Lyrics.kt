package com.athenacoree.music

/** A timestamped line imported from a user-owned LRC file. */
internal data class MusicLyricsLine(val timeMs: Long, val text: String)

internal object MusicLyricsParser {
    private val timestamp = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")

    fun parse(source: String): List<MusicLyricsLine> {
        val result = mutableListOf<MusicLyricsLine>()
        for (raw in source.lineSequence()) {
            val matches = timestamp.findAll(raw).toList()
            if (matches.isEmpty()) continue
            val lyric = timestamp.replace(raw, "").trim()
            if (lyric.isBlank()) continue
            for (match in matches) {
                val minutes = match.groupValues[1].toLongOrNull() ?: continue
                val seconds = match.groupValues[2].toLongOrNull() ?: continue
                val fraction = match.groupValues[3]
                val millis = when (fraction.length) {
                    1 -> (fraction.toLongOrNull() ?: 0L) * 100L
                    2 -> (fraction.toLongOrNull() ?: 0L) * 10L
                    3 -> fraction.toLongOrNull() ?: 0L
                    else -> 0L
                }
                if (seconds < 60) result += MusicLyricsLine((minutes * 60 + seconds) * 1000 + millis, lyric)
            }
        }
        return result.sortedBy { it.timeMs }
    }
}

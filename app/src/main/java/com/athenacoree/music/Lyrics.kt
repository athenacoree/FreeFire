package com.athenacoree.music

/** A timestamped line imported from a user-owned LRC file. */
internal data class MusicLyricsLine(val timeMs: Long, val text: String)

internal object MusicLyricsParser {
    private val timestamp = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val offsetTag = Regex("""\[offset:([+-]?\d+)]""", RegexOption.IGNORE_CASE)

    fun parse(source: String): List<MusicLyricsLine> {
        val result = mutableListOf<MusicLyricsLine>()
        val offsetMs = offsetTag.findAll(source).lastOrNull()?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
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
                if (seconds < 60) {
                    val timestampMs = (minutes * 60 + seconds) * 1000 + millis
                    result += MusicLyricsLine((timestampMs + offsetMs).coerceAtLeast(0L), lyric)
                }
            }
        }
        return result.sortedBy { it.timeMs }
    }
}

internal object MusicLyricsStorage {
    private const val PREFS = "music_lyrics"

    private fun key(mediaId: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(mediaId.toByteArray(Charsets.UTF_8))
        return "lrc_" + digest.joinToString("") { "%02x".format(it) }
    }

    fun save(context: android.content.Context, mediaId: String, source: String) {
        if (mediaId.isBlank()) return
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putString(key(mediaId), source).apply()
    }

    fun load(context: android.content.Context, mediaId: String): List<MusicLyricsLine> {
        if (mediaId.isBlank()) return emptyList()
        val source = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .getString(key(mediaId), null) ?: return emptyList()
        return MusicLyricsParser.parse(source)
    }
}

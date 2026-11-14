package com.athenacoree.music

import android.content.Context
import java.security.MessageDigest

/** A timestamped line imported from a user-owned LRC file. */
internal data class MusicLyricsLine(val timeMs: Long, val text: String)

internal object MusicLyricsParser {
    private const val MAX_SOURCE_CHARS = 64 * 1024
    private const val MAX_LYRIC_LINES = 4_000
    private val timestamp = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val offsetTag = Regex("""\[offset:([+-]?\d+)]""", RegexOption.IGNORE_CASE)

    fun parse(source: String): List<MusicLyricsLine> {
        val boundedSource = source.take(MAX_SOURCE_CHARS)
        val offsetMs = offsetTag.findAll(boundedSource).lastOrNull()
            ?.groupValues?.getOrNull(1)?.toLongOrNull()?.coerceIn(-600_000L, 600_000L) ?: 0L
        val result = ArrayList<MusicLyricsLine>(minOf(256, MAX_LYRIC_LINES))

        for (raw in boundedSource.lineSequence()) {
            if (result.size >= MAX_LYRIC_LINES) break
            val matches = timestamp.findAll(raw)
            val lyric = timestamp.replace(raw, "").trim()
            if (lyric.isBlank()) continue

            for (match in matches) {
                if (result.size >= MAX_LYRIC_LINES) break
                val minutes = match.groupValues[1].toLongOrNull() ?: continue
                val seconds = match.groupValues[2].toLongOrNull() ?: continue
                val fraction = match.groupValues[3]
                val millis = when (fraction.length) {
                    1 -> (fraction.toLongOrNull() ?: 0L) * 100L
                    2 -> (fraction.toLongOrNull() ?: 0L) * 10L
                    3 -> fraction.toLongOrNull() ?: 0L
                    else -> 0L
                }
                if (seconds !in 0..59 || minutes > 1_000_000L) continue
                val timestampMs = (minutes * 60L + seconds) * 1000L + millis
                result += MusicLyricsLine((timestampMs + offsetMs).coerceAtLeast(0L), lyric)
            }
        }
        return result.sortedBy { it.timeMs }
    }
}

/** Stores only lyrics explicitly imported by the listener, locally on this device. */
internal object MusicLyricsStorage {
    private const val PREFS = "music_lyrics"
    private const val MAX_LYRICS_CHARS = 64 * 1024

    fun save(context: Context, mediaId: String, source: String) {
        if (mediaId.isBlank()) return
        val safeText = source.take(MAX_LYRICS_CHARS)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(storageKey(mediaId), safeText)
            .apply()
    }

    fun load(context: Context, mediaId: String): List<MusicLyricsLine> {
        if (mediaId.isBlank()) return emptyList()
        val source = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(storageKey(mediaId), null) ?: return emptyList()
        return MusicLyricsParser.parse(source)
    }

    private fun storageKey(mediaId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(mediaId.toByteArray(Charsets.UTF_8))
        return "lyrics_" + digest.take(16).joinToString("") { "%02x".format(it) }
    }
}

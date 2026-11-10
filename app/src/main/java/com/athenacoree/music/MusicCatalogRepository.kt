package com.athenacoree.music

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
private data class CatalogId(val id: String)

internal data class CatalogSummary(
    val artists: Int,
    val albums: Int,
    val tracks: Int,
    val videos: Int
)

/**
 * Checks the public catalog without downloading every complete record.
 * The summary is capped to a generous sample so the profile screen stays light.
 */
internal object MusicCatalogRepository {
    private const val SUMMARY_LIMIT = 1000

    suspend fun loadSummary(): CatalogSummary {
        return CatalogSummary(
            artists = countRows("artists"),
            albums = countRows("albums"),
            tracks = countRows("tracks"),
            videos = countRows("videos")
        )
    }

    private suspend fun countRows(table: String): Int {
        return MusicSupabase.client
            .from(table)
            .select {
                limit(count = SUMMARY_LIMIT)
            }
            .decodeList<CatalogId>()
            .size
    }
}

package com.athenacoree.music

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.count
import kotlinx.serialization.Serializable

internal data class CatalogSummary(
    val artists: Int,
    val albums: Int,
    val tracks: Int,
    val videos: Int
)

@Serializable
private data class CatalogId(
    val id: String
)

/**
 * Uses PostgREST exact counts rather than downloading every id. This avoids
 * the default row limit making large catalogs appear smaller than they are.
 */
internal object MusicCatalogRepository {
    suspend fun loadSummary(): CatalogSummary {
        return CatalogSummary(
            artists = countRows("artists"),
            albums = countRows("albums"),
            tracks = countRows("tracks"),
            videos = countRows("videos")
        )
    }

    private suspend fun countRows(table: String): Int {
        val count = MusicSupabase.client
            .from(table)
            .select { count(Count.EXACT) }
            .countOrNull()
        return count?.toInt() ?: 0
    }
}

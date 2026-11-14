package com.athenacoree.music

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
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
 * The summary counts every row through small ID-only pages.
 */
internal object MusicCatalogRepository {
    private const val PAGE_SIZE = 500L

    suspend fun loadSummary(): CatalogSummary {
        return CatalogSummary(
            artists = countRows("artists"),
            albums = countRows("albums"),
            tracks = countRows("tracks"),
            videos = countRows("videos")
        )
    }

    private suspend fun countRows(table: String): Int {
        var offset = 0L
        var total = 0
        while (true) {
            val page = MusicSupabase.client.from(table).select(columns = Columns.list("id")) {
                order(column = "id", order = Order.ASCENDING)
                range(offset..(offset + PAGE_SIZE - 1))
            }.decodeList<CatalogId>()
            if (page.isEmpty()) break
            total += page.size
            if (page.size < PAGE_SIZE) break
            offset += page.size
        }
        return total
    }
}

package com.athenacoree.music

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class ArtistDto(
    val id: String,
    val name: String,
    val biography: String? = null,
    @SerialName("image_path") val imagePath: String? = null,
    val version: Long = 1,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
private data class AlbumDto(
    val id: String,
    @SerialName("artist_id") val artistId: String? = null,
    val title: String,
    @SerialName("artwork_path") val artworkPath: String? = null,
    val version: Long = 1,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
private data class TrackDto(
    val id: String,
    @SerialName("artist_id") val artistId: String? = null,
    @SerialName("album_id") val albumId: String? = null,
    val title: String,
    @SerialName("duration_ms") val durationMs: Long? = null,
    @SerialName("media_source") val mediaSource: String? = null,
    @SerialName("artwork_path") val artworkPath: String? = null,
    @SerialName("content_type") val contentType: String = "audio",
    val version: Long = 1,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
private data class VideoDto(
    val id: String,
    @SerialName("artist_id") val artistId: String? = null,
    val title: String,
    @SerialName("thumbnail_path") val thumbnailPath: String? = null,
    val provider: String,
    @SerialName("provider_video_id") val providerVideoId: String? = null,
    @SerialName("authorized_media_path") val authorizedMediaPath: String? = null,
    val version: Long = 1,
    @SerialName("updated_at") val updatedAt: String = ""
)

internal data class CatalogSyncResult(
    val artists: Int,
    val albums: Int,
    val tracks: Int,
    val videos: Int
)

internal class MusicCatalogSyncRepository(private val dao: MusicLibraryDao) {
    private companion object {
        const val PAGE_SIZE = 500L
    }

    private suspend inline fun <reified T> syncPages(
        table: String,
        crossinline toEntry: (T) -> CatalogEntryEntity,
        crossinline onPage: (List<T>) -> Unit = {}
    ): Int {
        var offset = 0L
        var total = 0
        while (true) {
            val page = MusicSupabase.client.from(table)
                .select {
                    order(column = "id", order = Order.ASCENDING)
                    range(offset..(offset + PAGE_SIZE - 1))
                }
                .decodeList<T>()
            if (page.isEmpty()) break
            onPage(page)
            dao.upsertCatalogEntriesIfNewer(page.map(toEntry))
            total += page.size
            if (page.size < PAGE_SIZE) break
            offset += page.size
        }
        return total
    }

    suspend fun syncAll(): CatalogSyncResult {
        val artistNames = mutableMapOf<String, String>()
        val artists = syncPages<ArtistDto>(
            table = "artists",
            toEntry = { item ->
                CatalogEntryEntity(
                    item.id, "artist", item.name, item.biography.orEmpty(),
                    item.imagePath, version = item.version, updatedAt = item.updatedAt
                )
            },
            onPage = { page -> page.forEach { artistNames[it.id] = it.name } }
        )
        val albums = syncPages<AlbumDto>(table = "albums", toEntry = { item ->
            CatalogEntryEntity(
                item.id, "album", item.title,
                item.artistId?.let { artistNames[it] }.orEmpty(), item.artworkPath,
                version = item.version, updatedAt = item.updatedAt, artistId = item.artistId
            )
        })
        val tracks = syncPages<TrackDto>(table = "tracks", toEntry = { item ->
            CatalogEntryEntity(
                item.id, "track", item.title,
                item.artistId?.let { artistNames[it] }.orEmpty(), item.artworkPath,
                item.contentType, item.albumId, item.mediaSource, item.version, item.updatedAt,
                artistId = item.artistId, albumId = item.albumId
            )
        })
        val videos = syncPages<VideoDto>(table = "videos", toEntry = { item ->
            CatalogEntryEntity(
                item.id, "video", item.title,
                item.artistId?.let { artistNames[it] }.orEmpty(), item.thumbnailPath,
                item.provider, item.providerVideoId, item.authorizedMediaPath,
                item.version, item.updatedAt, artistId = item.artistId
            )
        })
        return CatalogSyncResult(artists, albums, tracks, videos)
    }
}

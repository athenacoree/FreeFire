package com.athenacoree.music

import io.github.jan.supabase.postgrest.from
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
    suspend fun syncAll(): CatalogSyncResult {
        val client = MusicSupabase.client
        val artists = client.from("artists").select().decodeList<ArtistDto>()
        val artistNames = artists.associate { it.id to it.name }
        artists.forEach { item ->
            dao.upsertCatalogEntryIfNewer(CatalogEntryEntity(item.id, "artist", item.name, item.biography.orEmpty(),
                item.imagePath, version = item.version, updatedAt = item.updatedAt))
        }

        val albums = client.from("albums").select().decodeList<AlbumDto>()
        albums.forEach { item ->
            dao.upsertCatalogEntryIfNewer(CatalogEntryEntity(item.id, "album", item.title,
                item.artistId?.let { artistNames[it] }.orEmpty(), item.artworkPath,
                version = item.version, updatedAt = item.updatedAt))
        }

        val tracks = client.from("tracks").select().decodeList<TrackDto>()
        tracks.forEach { item ->
            dao.upsertCatalogEntryIfNewer(CatalogEntryEntity(item.id, "track", item.title,
                item.artistId?.let { artistNames[it] }.orEmpty(), item.artworkPath,
                item.contentType, item.albumId, item.mediaSource, item.version, item.updatedAt))
        }

        val videos = client.from("videos").select().decodeList<VideoDto>()
        videos.forEach { item ->
            dao.upsertCatalogEntryIfNewer(CatalogEntryEntity(item.id, "video", item.title,
                item.artistId?.let { artistNames[it] }.orEmpty(), item.thumbnailPath,
                item.provider, item.providerVideoId, item.authorizedMediaPath,
                item.version, item.updatedAt))
        }

        return CatalogSyncResult(artists.size, albums.size, tracks.size, videos.size)
    }
}

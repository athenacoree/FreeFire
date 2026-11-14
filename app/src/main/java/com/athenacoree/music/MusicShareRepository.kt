package com.athenacoree.music

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class CreateMusicShareLinkParams(
    @SerialName("p_resource_type") val resourceType: String,
    @SerialName("p_resource_id") val resourceId: String
)

@Serializable
private data class GetMusicSharedContentParams(
    @SerialName("p_token") val token: String
)

@Serializable
internal data class MusicLocalPlaylistShareTrack(
    val id: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    @SerialName("duration_ms") val durationMs: Long? = null,
    @SerialName("content_type") val contentType: String = "audio"
)

@Serializable
private data class CreateLocalPlaylistShareParams(
    @SerialName("p_title") val title: String,
    @SerialName("p_description") val description: String,
    @SerialName("p_tracks") val tracks: List<MusicLocalPlaylistShareTrack>
)

@Serializable
internal data class MusicSharedTrack(
    val id: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("duration_ms") val durationMs: Long? = null,
    @SerialName("content_type") val contentType: String = "audio"
)

@Serializable
internal data class MusicSharedContent(
    val type: String,
    val id: String,
    val title: String,
    val description: String = "",
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("is_collaborative") val isCollaborative: Boolean = false,
    @SerialName("artist_id") val artistId: String? = null,
    @SerialName("album_id") val albumId: String? = null,
    @SerialName("duration_ms") val durationMs: Long? = null,
    @SerialName("content_type") val contentType: String? = null,
    val provider: String? = null,
    @SerialName("provider_video_id") val providerVideoId: String? = null,
    val tracks: List<MusicSharedTrack> = emptyList()
)

internal object MusicShareRepository {
    private const val SHARE_BASE_URL =
        "https://wlbnulfixkfmvcifvpum.supabase.co/functions/v1/music-share/"

    suspend fun create(resourceType: String, resourceId: String): String {
        require(resourceType in setOf("artist", "album", "track", "video", "playlist"))
        return MusicSupabase.client.postgrest.rpc(
            "create_music_share_link",
            CreateMusicShareLinkParams(resourceType, resourceId)
        ).decodeAs<String>()
    }

    suspend fun createLocalPlaylist(
        title: String,
        description: String,
        tracks: List<LocalTrackEntity>
    ): String {
        require(tracks.size <= 200) { "Puedes compartir listas de hasta 200 canciones. Divide esta lista en dos para compartirla." }
        val safeTracks = tracks.map { track ->
            MusicLocalPlaylistShareTrack(
                id = track.id.take(80),
                title = track.title.take(180),
                artist = track.artistName.take(120),
                album = track.albumTitle.take(120),
                durationMs = track.durationMs,
                contentType = if (track.source == "device-video") "video" else "audio"
            )
        }
        return MusicSupabase.client.postgrest.rpc(
            "create_music_local_playlist_share",
            CreateLocalPlaylistShareParams(title.trim(), description.trim().take(500), safeTracks)
        ).decodeAs<String>()
    }

    suspend fun load(token: String): MusicSharedContent {
        require(token.matches(Regex("^[0-9a-f]{64}$")))
        return MusicSupabase.client.postgrest.rpc(
            "get_music_shared_content",
            GetMusicSharedContentParams(token)
        ).decodeAs<MusicSharedContent>()
    }

    fun publicUrl(token: String): String = SHARE_BASE_URL + token
}

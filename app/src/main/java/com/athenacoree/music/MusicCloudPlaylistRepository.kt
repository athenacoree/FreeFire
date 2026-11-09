package com.athenacoree.music

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CloudPlaylistDto(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    val description: String? = null,
    @SerialName("is_collaborative") val isCollaborative: Boolean = false
)

@Serializable
private data class CloudPlaylistItemInsert(
    @SerialName("playlist_id") val playlistId: String,
    @SerialName("track_id") val trackId: String,
    @SerialName("added_by") val addedBy: String,
    val position: Int = 0
)

@Serializable
private data class CloudPlaylistInsert(
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    val description: String,
    @SerialName("is_collaborative") val isCollaborative: Boolean
)

@Serializable
private data class InviteMemberParams(
    @SerialName("p_playlist_id") val playlistId: String,
    @SerialName("p_user_id") val userId: String,
    @SerialName("p_role") val role: String
)

internal object MusicCloudPlaylistRepository {
    suspend fun loadMyPlaylists(): List<CloudPlaylistDto> {
        if (MusicSupabase.client.gotrue.currentSessionOrNull() == null) return emptyList()
        return MusicSupabase.client.from("playlists").select().decodeList<CloudPlaylistDto>()
    }

    suspend fun create(name: String, description: String, collaborative: Boolean) {
        val userId = MusicSupabase.client.gotrue.currentSessionOrNull()?.user?.id
            ?: error("Inicia sesión para crear una lista compartida.")
        MusicSupabase.client.from("playlists").insert(
            CloudPlaylistInsert(userId, name.trim(), description.trim(), collaborative)
        )
    }

    suspend fun addTrack(playlistId: String, trackId: String) {
        val userId = MusicSupabase.client.gotrue.currentSessionOrNull()?.user?.id
            ?: error("Inicia sesión para modificar una lista compartida.")
        MusicSupabase.client.from("playlist_items").insert(
            CloudPlaylistItemInsert(playlistId, trackId, userId)
        )
    }

    suspend fun invite(playlistId: String, userId: String, role: String) {
        require(role == "viewer" || role == "editor")
        MusicSupabase.client.postgrest.rpc(
            "music_invite_playlist_member",
            InviteMemberParams(playlistId, userId, role)
        )
    }
}

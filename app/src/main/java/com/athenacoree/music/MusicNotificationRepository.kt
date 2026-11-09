package com.athenacoree.music

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MusicNotificationDto(
    val id: String,
    val category: String,
    val title: String,
    val body: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("read_at") val readAt: String? = null
)

internal object MusicNotificationRepository {
    suspend fun loadInbox(): List<MusicNotificationDto> {
        if (MusicSupabase.client.gotrue.currentSessionOrNull() == null) return emptyList()
        return MusicSupabase.client.from("notifications").select()
            .decodeList<MusicNotificationDto>()
            .sortedByDescending { it.createdAt }
    }
}

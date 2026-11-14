package com.athenacoree.music

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class MarkMusicNotificationReadParams(
    @SerialName("p_notification_id") val notificationId: String
)

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
        if (MusicSupabase.client.auth.currentSessionOrNull() == null) return emptyList()
        return MusicSupabase.client.from("notifications").select()
            .decodeList<MusicNotificationDto>()
            .sortedByDescending { it.createdAt }
    }

    suspend fun markAsRead(notificationId: String) {
        require(notificationId.isNotBlank())
        MusicSupabase.client.postgrest.rpc(
            "music_mark_notification_read",
            MarkMusicNotificationReadParams(notificationId)
        ).decodeAs<Boolean>()
    }
}

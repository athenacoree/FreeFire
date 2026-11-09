package com.athenacoree.music

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class DeviceScanResult(val imported: Int, val hasMore: Boolean, val scanned: Int)

internal object MusicDeviceScanner {
    private const val BATCH_SIZE = 5
    private const val PREFS = "music_device_scan"
    private const val OFFSET = "media_offset"

    suspend fun scanNextMediaBatch(context: Context, repository: MusicLibraryRepository): DeviceScanResult =
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val offset = prefs.getInt(OFFSET, 0)
            val collection = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.MediaColumns.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.MediaColumns.DURATION
            )
            val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
            val args = arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )
            val sort = "${MediaStore.MediaColumns.DATE_ADDED} DESC LIMIT $BATCH_SIZE OFFSET $offset"
            val found = mutableListOf<LocalTrackEntity>()
            context.contentResolver.query(collection, projection, selection, args, sort)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val titleCol = cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
                while (cursor.moveToNext()) {
                    val uri = ContentUris.withAppendedId(collection, cursor.getLong(idCol))
                    val video = cursor.getInt(typeCol) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    val title = if (titleCol >= 0) cursor.getString(titleCol).orEmpty() else ""
                    val artist = if (artistCol >= 0) cursor.getString(artistCol).orEmpty() else ""
                    val album = if (albumCol >= 0) cursor.getString(albumCol).orEmpty() else ""
                    val duration = if (durationCol >= 0 && !cursor.isNull(durationCol)) cursor.getLong(durationCol) else null
                    found += LocalTrackEntity(
                        id = uri.toString(), title = title.ifBlank { "Archivo multimedia" },
                        artistName = artist.takeUnless { it == "<unknown>" }.orEmpty(),
                        albumTitle = album, mediaUri = uri.toString(),
                        source = if (video) "device-video" else "device-audio", durationMs = duration
                    )
                }
            }
            if (found.isNotEmpty()) repository.addTracks(found)
            prefs.edit().putInt(OFFSET, offset + found.size).apply()
            DeviceScanResult(found.size, found.size == BATCH_SIZE, offset + found.size)
        }

    fun resetMediaScan(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(OFFSET).apply()
    }
}
package com.athenacoree.music

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class DeviceScanResult(val imported: Int, val hasMore: Boolean, val scanned: Int)

internal object MusicDeviceScanner {
    private const val BATCH_SIZE = 100
    private const val PREFS = "music_device_scan"
    private const val OFFSET = "media_offset"
    private const val FILTER = "media_filter"

    suspend fun scanNextMediaBatch(
        context: Context,
        repository: MusicLibraryRepository,
        includeAudio: Boolean = true,
        includeVideo: Boolean = true
    ): DeviceScanResult = withContext(Dispatchers.IO) {
        val mediaTypes = buildList {
            if (includeAudio) add(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO)
            if (includeVideo) add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
        }
        if (mediaTypes.isEmpty()) return@withContext DeviceScanResult(0, false, 0)

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val filterKey = mediaTypes.joinToString(",")
        val previousFilter = prefs.getString(FILTER, null)
        val offset = if (previousFilter == filterKey) prefs.getInt(OFFSET, 0) else 0

        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.MediaColumns.DURATION
        )
        val placeholders = mediaTypes.joinToString(",") { "?" }
        val queryArgs = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN ($placeholders)")
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, mediaTypes.map { it.toString() }.toTypedArray())
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.MediaColumns.DATE_ADDED))
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            putInt(ContentResolver.QUERY_ARG_LIMIT, BATCH_SIZE)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
        }

        val found = mutableListOf<LocalTrackEntity>()
        context.contentResolver.query(collection, projection, queryArgs, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val titleCol = cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)
            val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)

            while (cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(collection, cursor.getLong(idCol))
                val isVideo = cursor.getInt(typeCol) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val title = if (titleCol >= 0) cursor.getString(titleCol).orEmpty() else ""
                val artist = if (artistCol >= 0) cursor.getString(artistCol).orEmpty() else ""
                val album = if (albumCol >= 0) cursor.getString(albumCol).orEmpty() else ""
                val duration = if (durationCol >= 0 && !cursor.isNull(durationCol)) cursor.getLong(durationCol) else null
                found += LocalTrackEntity(
                    id = uri.toString(),
                    title = title.ifBlank { "Archivo multimedia" },
                    artistName = artist.takeUnless { it == "<unknown>" }.orEmpty(),
                    albumTitle = album,
                    mediaUri = uri.toString(),
                    source = if (isVideo) "device-video" else "device-audio",
                    durationMs = duration
                )
            }
        }

        if (found.isNotEmpty()) repository.addTracks(found)

        val nextOffset = offset + found.size
        prefs.edit().putString(FILTER, filterKey).apply()
        if (found.size == BATCH_SIZE) {
            prefs.edit().putInt(OFFSET, nextOffset).apply()
        } else {
            // A completed scan starts again at the newest media next time, so new files are discovered.
            prefs.edit().remove(OFFSET).apply()
        }
        DeviceScanResult(found.size, found.size == BATCH_SIZE, nextOffset)
    }

    fun resetMediaScan(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(OFFSET).remove(FILTER).apply()
    }
}

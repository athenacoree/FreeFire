package com.athenacoree.music

import kotlinx.coroutines.flow.Flow

/**
 * Local-first library API. Guest data is stored only on this device.
 * Cloud sync is intentionally a separate future layer and never overwrites local files.
 */
internal class MusicLibraryRepository(
    private val dao: MusicLibraryDao
) {
    val tracks: Flow<List<LocalTrackEntity>> = dao.observeTracks()
    val favorites: Flow<List<LocalTrackEntity>> = dao.observeFavorites()
    val playlists: Flow<List<LocalPlaylistEntity>> = dao.observePlaylists()
    val trackCount: Flow<Int> = dao.observeTrackCount()

    fun search(query: String): Flow<List<LocalTrackEntity>> = dao.searchTracks(query.trim())

    fun catalog(kind: String): Flow<List<CatalogEntryEntity>> = dao.observeCatalog(kind)

    internal val databaseDao: MusicLibraryDao get() = dao

    suspend fun addTrack(track: LocalTrackEntity) = dao.saveTrack(track)

    suspend fun addTracks(tracks: List<LocalTrackEntity>) = dao.saveTracks(tracks)

    suspend fun setFavorite(trackId: String, favorite: Boolean) =
        dao.setFavorite(trackId, favorite)

    suspend fun createPlaylist(name: String, description: String = "") {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Playlist name cannot be empty." }
        val now = System.currentTimeMillis()
        dao.savePlaylist(
            LocalPlaylistEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = cleanName,
                description = description.trim(),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun addToPlaylist(playlistId: String, trackId: String, position: Int = 0) =
        dao.addTrackToPlaylist(LocalPlaylistTrackEntity(playlistId, trackId, position))

    fun playlistTracks(playlistId: String): Flow<List<LocalTrackEntity>> =
        dao.observePlaylistTracks(playlistId)

    suspend fun removeFromPlaylist(playlistId: String, trackId: String) =
        dao.removeTrackFromPlaylist(playlistId, trackId)

    suspend fun removeTrack(trackId: String) = dao.deleteTrack(trackId)
}

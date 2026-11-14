package com.athenacoree.music

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

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
    val favoriteCount: Flow<Int> = dao.observeFavoriteCount()
    val playbackCount: Flow<Int> = dao.observePlaybackCount()
    val recentPlayback: Flow<List<PlaybackHistoryEntity>> = dao.observeRecentPlayback()
    val recentlyPlayedTracks: Flow<List<LocalTrackEntity>> = dao.observeRecentlyPlayedTracks()
    val mostPlayed: Flow<List<PlaybackStat>> = dao.observeMostPlayed()

    fun search(query: String): Flow<List<LocalTrackEntity>> = flow {
        delay(180)
        emitAll(dao.searchTracks(query.trim()))
    }

    fun searchFavorites(query: String): Flow<List<LocalTrackEntity>> = flow {
        delay(180)
        emitAll(dao.searchFavoriteTracks(query.trim()))
    }

    fun catalog(kind: String): Flow<List<CatalogEntryEntity>> = dao.observeCatalog(kind)

    fun searchCatalog(query: String): Flow<List<CatalogEntryEntity>> = dao.searchCatalog(query)

    internal val databaseDao: MusicLibraryDao get() = dao

    suspend fun recordPlayback(track: LocalTrackEntity) = dao.recordPlayback(
        PlaybackHistoryEntity(
            id = java.util.UUID.randomUUID().toString(),
            trackId = track.id,
            title = track.title,
            artistName = track.artistName
        )
    )

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

    fun catalogByArtist(artistId: String, kind: String): Flow<List<CatalogEntryEntity>> =
        dao.observeCatalogByArtist(artistId, kind)

    fun catalogByAlbum(albumId: String): Flow<List<CatalogEntryEntity>> =
        dao.observeCatalogByAlbum(albumId, "track")

    suspend fun removeFromPlaylist(playlistId: String, trackId: String) =
        dao.removeTrackFromPlaylist(playlistId, trackId)

    suspend fun removeTrack(trackId: String) = dao.deleteTrack(trackId)
}

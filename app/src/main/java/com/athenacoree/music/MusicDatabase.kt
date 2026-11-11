package com.athenacoree.music

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_tracks", indices = [Index(value = ["title"]), Index(value = ["artistName"])])
data class LocalTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistName: String = "",
    val albumTitle: String = "",
    val artworkUri: String? = null,
    val mediaUri: String? = null,
    val source: String = "catalog",
    val durationMs: Long? = null,
    val isFavorite: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)


@Entity(tableName = "playback_history", indices = [Index(value = ["trackId"]), Index(value = ["playedAt"])])
data class PlaybackHistoryEntity(
    @PrimaryKey val id: String,
    val trackId: String,
    val title: String,
    val artistName: String = "",
    val playedAt: Long = System.currentTimeMillis()
)

data class PlaybackStat(
    val trackId: String,
    val title: String,
    val artistName: String,
    val playCount: Int
)

@Entity(tableName = "catalog_entries", indices = [Index(value = ["kind"]), Index(value = ["updatedAt"])])
data class CatalogEntryEntity(@PrimaryKey val id: String, val kind: String, val title: String, val subtitle: String = "", val imagePath: String? = null, val provider: String? = null, val providerId: String? = null, val mediaPath: String? = null, val version: Long = 1, val updatedAt: String = "")

@Entity(tableName = "local_playlists")
data class LocalPlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "local_playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index("trackId")]
)
data class LocalPlaylistTrackEntity(
    val playlistId: String,
    val trackId: String,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface MusicLibraryDao {
    @Query("SELECT * FROM local_tracks ORDER BY addedAt DESC")
    fun observeTracks(): Flow<List<LocalTrackEntity>>

    @Query("SELECT * FROM local_tracks WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<LocalTrackEntity>>

    @Query("SELECT * FROM local_tracks WHERE title LIKE '%' || :query || '%' OR artistName LIKE '%' || :query || '%' ORDER BY title")
    fun searchTracks(query: String): Flow<List<LocalTrackEntity>>

    @Query("SELECT * FROM local_tracks WHERE isFavorite = 1 AND (title LIKE '%' || :query || '%' OR artistName LIKE '%' || :query || '%') ORDER BY addedAt DESC")
    fun searchFavoriteTracks(query: String): Flow<List<LocalTrackEntity>>

    @Query("SELECT t.* FROM local_tracks t INNER JOIN (SELECT trackId, MAX(playedAt) AS lastPlayed FROM playback_history GROUP BY trackId) recent ON recent.trackId = t.id ORDER BY recent.lastPlayed DESC LIMIT 4")
    fun observeRecentlyPlayedTracks(): Flow<List<LocalTrackEntity>>

    @Query("SELECT COUNT(*) FROM local_tracks")
    fun observeTrackCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM local_tracks WHERE isFavorite = 1")
    fun observeFavoriteCount(): Flow<Int>

    @Query("SELECT * FROM local_tracks WHERE mediaUri = :mediaUri LIMIT 1")
    suspend fun findTrackByMediaUri(mediaUri: String): LocalTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTrack(track: LocalTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTracks(tracks: List<LocalTrackEntity>)

    @Query("UPDATE local_tracks SET isFavorite = :favorite WHERE id = :trackId")
    suspend fun setFavorite(trackId: String, favorite: Boolean)

    @Query("DELETE FROM local_tracks WHERE id = :trackId")
    suspend fun deleteTrack(trackId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayback(item: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE id NOT IN (SELECT id FROM playback_history ORDER BY playedAt DESC LIMIT 5000)")
    suspend fun trimPlaybackHistory()

    @Transaction
    suspend fun recordPlayback(item: PlaybackHistoryEntity) {
        insertPlayback(item)
        trimPlaybackHistory()
    }

    @Query("SELECT COUNT(*) FROM playback_history")
    fun observePlaybackCount(): Flow<Int>

    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT 5")
    fun observeRecentPlayback(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT trackId, title, artistName, COUNT(*) AS playCount FROM playback_history GROUP BY trackId, title, artistName ORDER BY playCount DESC LIMIT 5")
    fun observeMostPlayed(): Flow<List<PlaybackStat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlaylist(playlist: LocalPlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(item: LocalPlaylistTrackEntity)

    @Query("SELECT * FROM catalog_entries WHERE kind = :kind ORDER BY title COLLATE NOCASE")
    fun observeCatalog(kind: String): Flow<List<CatalogEntryEntity>>

    @Query("SELECT * FROM catalog_entries WHERE kind = :kind ORDER BY title COLLATE NOCASE")
    suspend fun getCatalog(kind: String): List<CatalogEntryEntity>

    @Query("SELECT * FROM catalog_entries WHERE id = :id LIMIT 1")
    suspend fun getCatalogEntry(id: String): CatalogEntryEntity?

    @Query("SELECT * FROM catalog_entries WHERE id IN (:ids)")
    suspend fun getCatalogEntriesByIds(ids: List<String>): List<CatalogEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCatalogEntry(entry: CatalogEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCatalogEntries(entries: List<CatalogEntryEntity>)

    @Transaction
    suspend fun upsertCatalogEntriesIfNewer(entries: List<CatalogEntryEntity>) {
        if (entries.isEmpty()) return
        val existing = getCatalogEntriesByIds(entries.map { it.id }).associateBy { it.id }
        val changed = entries.filter { incoming ->
            val current = existing[incoming.id]
            current == null || incoming.version >= current.version
        }
        if (changed.isNotEmpty()) saveCatalogEntries(changed)
    }

    @Transaction
    suspend fun upsertCatalogEntryIfNewer(entry: CatalogEntryEntity) {
        val current = getCatalogEntry(entry.id)
        if (current == null || entry.version >= current.version) {
            saveCatalogEntry(entry)
        }
    }

    @Query("SELECT * FROM local_playlists ORDER BY updatedAt DESC")
    fun observePlaylists(): Flow<List<LocalPlaylistEntity>>

    @Query("SELECT t.* FROM local_tracks t INNER JOIN local_playlist_tracks p ON p.trackId = t.id WHERE p.playlistId = :playlistId ORDER BY p.position ASC")
    fun observePlaylistTracks(playlistId: String): Flow<List<LocalTrackEntity>>

    @Query("DELETE FROM local_playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)
}

@Database(
    entities = [LocalTrackEntity::class, LocalPlaylistEntity::class, LocalPlaylistTrackEntity::class, CatalogEntryEntity::class, PlaybackHistoryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun libraryDao(): MusicLibraryDao

    companion object {
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS catalog_entries (id TEXT NOT NULL PRIMARY KEY, kind TEXT NOT NULL, title TEXT NOT NULL, subtitle TEXT NOT NULL, imagePath TEXT, provider TEXT, providerId TEXT, mediaPath TEXT, version INTEGER NOT NULL, updatedAt TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_entries_kind ON catalog_entries(kind)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_entries_updatedAt ON catalog_entries(updatedAt)")
            }
        }
        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS playback_history (id TEXT NOT NULL PRIMARY KEY, trackId TEXT NOT NULL, title TEXT NOT NULL, artistName TEXT NOT NULL, playedAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_history_trackId ON playback_history(trackId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_history_playedAt ON playback_history(playedAt)")
            }
        }
        @Volatile private var INSTANCE: MusicDatabase? = null

        fun getInstance(context: Context): MusicDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music-library.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}

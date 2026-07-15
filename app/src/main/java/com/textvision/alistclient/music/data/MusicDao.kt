package com.textvision.alistclient.music.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @Query("SELECT * FROM music_artist ORDER BY name")
    fun artists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM music_album WHERE artist = :artist ORDER BY name")
    fun albumsByArtist(artist: String): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM music_album ORDER BY name")
    fun allAlbums(): Flow<List<AlbumEntity>>

    @Query(
        """
        SELECT * FROM music_album
        ORDER BY name DESC
        LIMIT :limit
        """,
    )
    fun recentAlbums(limit: Int): Flow<List<AlbumEntity>>

    @Query(
        """
        SELECT * FROM music_song
        WHERE artist = :artist AND album = :album
        ORDER BY trackNoInt ASC, trackNo ASC
        """,
    )
    fun songsByAlbum(artist: String, album: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM music_song ORDER BY artist, album, trackNoInt ASC, trackNo ASC")
    fun allSongs(): Flow<List<SongEntity>>

    /**
     * Look up songs whose paths match any entry in [paths]. Used by the playback
     * service to translate MediaItem mediaIds back into Song rows for UI display,
     * without paying the cost of `allSongs()` (a full table scan) every queue load.
     * The caller passes the small subset of paths that ExoPlayer currently holds
     * (typically ≤ a handful) so this returns at most a few rows.
     */
    @Query(
        """
        SELECT * FROM music_song
        WHERE path IN (:paths)
        """,
    )
    suspend fun songsByPaths(paths: List<String>): List<SongEntity>

    @Query(
        """
        SELECT a.artworkData FROM music_album a
        INNER JOIN music_song s ON s.artist = a.artist AND s.album = a.name
        WHERE s.path = :songPath LIMIT 1
        """,
    )
    suspend fun artworkForSong(songPath: String): ByteArray?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtists(items: List<ArtistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbums(items: List<AlbumEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(items: List<SongEntity>)

    @Query("DELETE FROM music_artist")
    suspend fun clearArtists()

    @Query("DELETE FROM music_album")
    suspend fun clearAlbums()

    @Query("DELETE FROM music_song")
    suspend fun clearSongs()

    @Query("SELECT COUNT(*) FROM music_song")
    suspend fun songCount(): Int
}

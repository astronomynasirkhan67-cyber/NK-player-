package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PlaybackHistoryItem
import com.example.data.model.Playlist
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.Song
import com.example.data.model.VideoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs ORDER BY playCount DESC, lastPlayedTimestamp DESC")
    fun getMostPlayedSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: String): Song?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongs(songs: List<Song>)

    @Update
    suspend fun updateSong(song: Song)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedTimestamp = :timestamp, firstPlayedTimestamp = CASE WHEN firstPlayedTimestamp = 0 THEN :timestamp ELSE firstPlayedTimestamp END WHERE id = :songId")
    suspend fun incrementPlayCount(songId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE songs SET totalListeningTimeSeconds = totalListeningTimeSeconds + :seconds WHERE id = :songId")
    suspend fun addListeningTime(songId: String, seconds: Long)

    @Query("UPDATE songs SET completionCount = completionCount + 1 WHERE id = :songId")
    suspend fun incrementSongCompletion(songId: String)

    @Query("UPDATE songs SET isFavorite = :isFav WHERE id = :songId")
    suspend fun setFavorite(songId: String, isFav: Boolean)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("DELETE FROM songs WHERE id LIKE 's%' AND length(id) <= 4 OR uri LIKE 'https://%' OR uri = ''")
    suspend fun deleteLegacyDemoSongs()

    @Query("SELECT id FROM songs")
    suspend fun getAllSongIds(): List<String>

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<String>)
}

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY title ASC")
    fun getAllVideos(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE isShort = 1 ORDER BY addedAt DESC")
    fun getShorts(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE isShort = 0 ORDER BY addedAt DESC")
    fun getLongVideos(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos ORDER BY playCount DESC, lastWatchedTimestamp DESC")
    fun getMostWatchedVideos(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteVideos(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: String): VideoItem?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideos(videos: List<VideoItem>)

    @Update
    suspend fun updateVideo(video: VideoItem)

    @Query("UPDATE videos SET playCount = playCount + 1, lastWatchedTimestamp = :timestamp, firstPlayedTimestamp = CASE WHEN firstPlayedTimestamp = 0 THEN :timestamp ELSE firstPlayedTimestamp END WHERE id = :videoId")
    suspend fun incrementWatchCount(videoId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE videos SET totalWatchTimeSeconds = totalWatchTimeSeconds + :seconds WHERE id = :videoId")
    suspend fun addWatchTime(videoId: String, seconds: Long)

    @Query("UPDATE videos SET completionCount = completionCount + 1 WHERE id = :videoId")
    suspend fun incrementVideoCompletion(videoId: String)

    @Query("UPDATE videos SET isFavorite = :isFav WHERE id = :videoId")
    suspend fun setFavorite(videoId: String, isFav: Boolean)

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getVideoCount(): Int

    @Query("DELETE FROM videos WHERE id LIKE 'v_%' OR uri LIKE 'https://%' OR uri = ''")
    suspend fun deleteLegacyDemoVideos()

    @Query("SELECT id FROM videos")
    suspend fun getAllVideoIds(): List<String>

    @Query("DELETE FROM videos WHERE id IN (:ids)")
    suspend fun deleteVideosByIds(ids: List<String>)
}

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<PlaybackHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: PlaybackHistoryItem)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    @Query("SELECT s.* FROM songs s INNER JOIN playlist_songs ps ON s.id = ps.songId WHERE ps.playlistId = :playlistId ORDER BY ps.addedAt ASC")
    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>>

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun getPlaylistCount(): Int

    @Query("DELETE FROM playlist_songs WHERE songId NOT IN (SELECT id FROM songs)")
    suspend fun cleanupOrphanedPlaylistSongs()
}

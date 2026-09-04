package com.example.data.repository

import com.example.data.local.PlaybackHistoryDao
import com.example.data.local.PlaylistDao
import com.example.data.local.SongDao
import com.example.data.local.VideoDao
import com.example.data.model.PlaybackHistoryItem
import com.example.data.model.Playlist
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.Song
import com.example.data.model.VideoItem
import kotlinx.coroutines.flow.Flow

class MusicRepository(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val videoDao: VideoDao,
    private val playbackHistoryDao: PlaybackHistoryDao
) {
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val mostPlayedSongs: Flow<List<Song>> = songDao.getMostPlayedSongs()
    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs()
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    val allVideos: Flow<List<VideoItem>> = videoDao.getAllVideos()
    val shorts: Flow<List<VideoItem>> = videoDao.getShorts()
    val longVideos: Flow<List<VideoItem>> = videoDao.getLongVideos()
    val mostWatchedVideos: Flow<List<VideoItem>> = videoDao.getMostWatchedVideos()
    val favoriteVideos: Flow<List<VideoItem>> = videoDao.getFavoriteVideos()

    val recentHistory: Flow<List<PlaybackHistoryItem>> = playbackHistoryDao.getRecentHistory()

    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> {
        return playlistDao.getSongsForPlaylist(playlistId)
    }

    suspend fun incrementPlayCount(songId: String) {
        songDao.incrementPlayCount(songId)
    }

    suspend fun addListeningTime(songId: String, seconds: Long) {
        songDao.addListeningTime(songId, seconds)
    }

    suspend fun incrementSongCompletion(songId: String) {
        songDao.incrementSongCompletion(songId)
    }

    suspend fun toggleFavorite(songId: String, currentFav: Boolean) {
        songDao.setFavorite(songId, !currentFav)
    }

    suspend fun incrementWatchCount(videoId: String) {
        videoDao.incrementWatchCount(videoId)
    }

    suspend fun addWatchTime(videoId: String, seconds: Long) {
        videoDao.addWatchTime(videoId, seconds)
    }

    suspend fun incrementVideoCompletion(videoId: String) {
        videoDao.incrementVideoCompletion(videoId)
    }

    suspend fun toggleVideoFavorite(videoId: String, currentFav: Boolean) {
        videoDao.setFavorite(videoId, !currentFav)
    }

    suspend fun logHistory(mediaId: String, mediaType: String, title: String, subtitle: String, durationSec: Int) {
        playbackHistoryDao.insertHistory(
            PlaybackHistoryItem(
                mediaId = mediaId,
                mediaType = mediaType,
                title = title,
                subtitle = subtitle,
                durationPlayedSeconds = durationSec
            )
        )
    }

    suspend fun clearHistory() {
        playbackHistoryDao.clearHistory()
    }

    suspend fun insertScannedSongs(songs: List<Song>) {
        songDao.insertSongs(songs)
    }

    suspend fun insertScannedVideos(videos: List<VideoItem>) {
        videoDao.insertVideos(videos)
    }

    suspend fun syncScannedMedia(scannedSongs: List<Song>, scannedVideos: List<VideoItem>) {
        // 1. Sync Songs
        if (scannedSongs.isNotEmpty()) {
            songDao.insertSongs(scannedSongs)
            val currentValidIds = scannedSongs.map { it.id }.toSet()
            val existingIds = songDao.getAllSongIds()
            val staleIds = existingIds.filter { it !in currentValidIds }
            if (staleIds.isNotEmpty()) {
                songDao.deleteSongsByIds(staleIds)
            }
        }

        // 2. Sync Videos
        if (scannedVideos.isNotEmpty()) {
            videoDao.insertVideos(scannedVideos)
            val currentValidIds = scannedVideos.map { it.id }.toSet()
            val existingIds = videoDao.getAllVideoIds()
            val staleIds = existingIds.filter { it !in currentValidIds }
            if (staleIds.isNotEmpty()) {
                videoDao.deleteVideosByIds(staleIds)
            }
        }

        // 3. Clean up orphaned playlist records
        playlistDao.cleanupOrphanedPlaylistSongs()
    }

    suspend fun createPlaylist(name: String, description: String): String {
        val id = "pl_${System.currentTimeMillis()}"
        playlistDao.insertPlaylist(
            Playlist(
                id = id,
                name = name,
                description = description,
                isSystem = false
            )
        )
        return id
    }

    suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String) {
        playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun deleteSong(songId: String) {
        songDao.deleteSongById(songId)
        playlistDao.cleanupOrphanedPlaylistSongs()
    }

    suspend fun updateSongTitle(songId: String, newTitle: String) {
        songDao.updateSongTitle(songId, newTitle)
    }

    suspend fun updateSongTitleAndUri(songId: String, newTitle: String, newUri: String) {
        songDao.updateSongTitleAndUri(songId, newTitle, newUri)
    }

    suspend fun deleteVideo(videoId: String) {
        videoDao.deleteVideoById(videoId)
    }

    suspend fun updateVideoTitle(videoId: String, newTitle: String) {
        videoDao.updateVideoTitle(videoId, newTitle)
    }

    suspend fun updateVideoTitleAndUri(videoId: String, newTitle: String, newUri: String) {
        videoDao.updateVideoTitleAndUri(videoId, newTitle, newUri)
    }

    suspend fun ensureInitialData() {
        // Purge any legacy demo or placeholder media
        try {
            songDao.deleteLegacyDemoSongs()
            videoDao.deleteLegacyDemoVideos()
            playlistDao.cleanupOrphanedPlaylistSongs()
        } catch (_: Exception) {
        }

        // Initialize default empty system playlists if none exist
        if (playlistDao.getPlaylistCount() == 0) {
            val initialPlaylists = listOf(
                Playlist(
                    id = "pl_most_played",
                    name = "🔥 Most Played",
                    description = "Top tracks ranked automatically by play count",
                    isSystem = true
                ),
                Playlist(
                    id = "pl_favorites",
                    name = "❤️ Favorites",
                    description = "Your marked favorite records",
                    isSystem = true
                )
            )
            for (pl in initialPlaylists) {
                playlistDao.insertPlaylist(pl)
            }
        }
    }
}

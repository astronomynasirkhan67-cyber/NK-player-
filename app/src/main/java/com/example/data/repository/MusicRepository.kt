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

    suspend fun ensureInitialData() {
        val count = songDao.getSongCount()
        if (count == 0) {
            val defaultSongs = listOf(
                Song(
                    id = "s1",
                    title = "Nasir Khan Anthem",
                    artist = "Nasir Khan",
                    album = "Electric Odyssey",
                    durationSeconds = 214,
                    coverResName = "img_cover_cyber",
                    genre = "Synth Pop / EDM",
                    bpm = 126,
                    playCount = 48,
                    isFavorite = true,
                    totalListeningTimeSeconds = 8560,
                    completionCount = 38,
                    lyrics = "[00:00] Synth intro builds with energy\n[00:15] Glowing neon pulses through the night\n[00:30] Feel the rhythm taking flight\n[00:45] Music for Nasir on the stereo loud\n[01:00] Turntable spins above the crowd\n[01:15] Electric pulse in every vein\n[01:30] Dance until the morning rain\n[01:45] Pure sonic bliss and harmony\n[02:00] Nasir Khan signature melody"
                ),
                Song(
                    id = "s2",
                    title = "Cyberpunk Night Drive",
                    artist = "Nasir Khan & Neon Pulse",
                    album = "Grid Horizon 2088",
                    durationSeconds = 198,
                    coverResName = "img_cover_cyber",
                    genre = "Synthwave",
                    bpm = 115,
                    playCount = 37,
                    isFavorite = true,
                    totalListeningTimeSeconds = 6200,
                    completionCount = 29,
                    lyrics = "[00:00] Cruising down the neon lane\n[00:20] Digital rain on holographic glass\n[00:40] Bassline rumbling smooth and deep\n[01:00] The city never falls asleep\n[01:20] Analog warmth in futuristic skies\n[01:40] Starlight reflection in your eyes"
                ),
                Song(
                    id = "s3",
                    title = "Rainy Tokyo Lo-Fi",
                    artist = "Nasir Khan",
                    album = "Midnight Coffee Beats",
                    durationSeconds = 184,
                    coverResName = "img_cover_lofi",
                    genre = "Chillhop / Lo-Fi",
                    bpm = 82,
                    playCount = 29,
                    isFavorite = false,
                    totalListeningTimeSeconds = 4800,
                    completionCount = 21,
                    lyrics = "[00:00] Soft vinyl crackle and warm Rhodes chords\n[00:22] Gentle raindrops tapping on the windowpane\n[00:44] Late night thoughts and steaming tea\n[01:06] Peaceful moments calm and free\n[01:28] Mellow melody soothing the mind\n[01:50] Sweet nostalgic feelings left behind"
                ),
                Song(
                    id = "s4",
                    title = "Golden Sunset Groove",
                    artist = "Nasir Khan ft. Luna",
                    album = "Velvet Horizons",
                    durationSeconds = 205,
                    coverResName = "img_cover_amber",
                    genre = "Nu-Disco / Funk",
                    bpm = 118,
                    playCount = 24,
                    isFavorite = true,
                    totalListeningTimeSeconds = 4100,
                    completionCount = 18,
                    lyrics = "[00:00] Slap bass groove kicks off the track\n[00:18] Sunset turns the ocean gold\n[00:36] Golden memories that never grow old\n[00:54] Move your feet under the evening sky\n[01:12] Catch the groove as the breeze goes by\n[01:30] Pure funky rhythm taking control"
                ),
                Song(
                    id = "s5",
                    title = "Deep Space Nebula",
                    artist = "Nasir Khan",
                    album = "Cosmic Reverie",
                    durationSeconds = 245,
                    coverResName = "img_cover_cosmic",
                    genre = "Ambient / Drone",
                    bpm = 70,
                    playCount = 19,
                    isFavorite = false,
                    totalListeningTimeSeconds = 3800,
                    completionCount = 14,
                    lyrics = "[00:00] Ethereal synth pad drifting into orbit\n[00:35] Distant stars whispering cosmic secrets\n[01:10] Weightless floating beyond the atmosphere\n[01:45] Endless galaxies appearing near\n[02:20] Harmonic silence and peaceful dreams"
                ),
                Song(
                    id = "s6",
                    title = "Subzero Echoes",
                    artist = "Nasir Khan & Cryo",
                    album = "Glacier Beats",
                    durationSeconds = 176,
                    coverResName = "img_cover_crystal",
                    genre = "Future Bass",
                    bpm = 135,
                    playCount = 16,
                    isFavorite = false,
                    totalListeningTimeSeconds = 2500,
                    completionCount = 11,
                    lyrics = "[00:00] Crystal chords chime in the cold\n[00:20] Glitchy percussion starts to roll\n[00:40] Explosive drop of icy synths\n[01:00] Echoes bouncing off frosty cliffs\n[01:20] Sparkling energy and crystal sound"
                ),
                Song(
                    id = "s7",
                    title = "Emerald Forest Breeze",
                    artist = "Nasir Khan",
                    album = "Organic Acoustic",
                    durationSeconds = 190,
                    coverResName = "img_cover_emerald",
                    genre = "Acoustic / Folk",
                    bpm = 96,
                    playCount = 12,
                    isFavorite = false,
                    totalListeningTimeSeconds = 1900,
                    completionCount = 9,
                    lyrics = "[00:00] Fingerpicked acoustic guitar melody\n[00:24] Gentle bird songs and rustling leaves\n[00:48] Warm afternoon sun filtering through\n[01:12] Fresh woodland air refreshing you\n[01:36] Calm stillness of the wild"
                ),
                Song(
                    id = "s8",
                    title = "Euphoric Astral Dance",
                    artist = "Nasir Khan",
                    album = "Electric Odyssey",
                    durationSeconds = 230,
                    coverResName = "img_cover_cyber",
                    genre = "Progressive Trance",
                    bpm = 128,
                    playCount = 9,
                    isFavorite = false,
                    totalListeningTimeSeconds = 1800,
                    completionCount = 7,
                    lyrics = "[00:00] Rhythmic arpeggio climbs higher\n[00:25] Bass drop builds with energy\n[00:50] Hands up in the festival night\n[01:15] Laser beams and neon light\n[01:40] Heartbeats synced with 128 BPM\n[02:05] Pure euphoria without an end"
                )
            )

            songDao.insertSongs(defaultSongs)

            val initialPlaylists = listOf(
                Playlist("pl_most_played", "🔥 Most Played Hits", "Top tracks ranked automatically by play count", isSystem = true),
                Playlist("pl_favorites", "❤️ Favorite Tracks", "Your personally marked favorite records", isSystem = true),
                Playlist("pl_chill", "🌙 Night Drive & Chill", "Smooth lo-fi, synthwave & relaxing atmospheric tracks", isSystem = false),
                Playlist("pl_energy", "⚡ High Energy Vibes", "Upbeat synth pop, funk and EDM to power your day", isSystem = false)
            )

            for (pl in initialPlaylists) {
                playlistDao.insertPlaylist(pl)
            }

            playlistDao.addSongToPlaylist(PlaylistSongCrossRef("pl_chill", "s2"))
            playlistDao.addSongToPlaylist(PlaylistSongCrossRef("pl_chill", "s3"))
            playlistDao.addSongToPlaylist(PlaylistSongCrossRef("pl_chill", "s4"))
            playlistDao.addSongToPlaylist(PlaylistSongCrossRef("pl_chill", "s5"))

            playlistDao.addSongToPlaylist(PlaylistSongCrossRef("pl_energy", "s1"))
            playlistDao.addSongToPlaylist(PlaylistSongCrossRef("pl_energy", "s6"))
            playlistDao.addSongToPlaylist(PlaylistSongCrossRef("pl_energy", "s8"))
        }

        val videoCount = videoDao.getVideoCount()
        if (videoCount == 0) {
            val sampleVideos = listOf(
                VideoItem(
                    id = "v_short_1",
                    title = "Nasir Live Studio Vinyl Scratching",
                    artist = "Nasir Khan",
                    uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    durationSeconds = 15,
                    isShort = true,
                    resolution = "1080x1920",
                    playCount = 62,
                    totalWatchTimeSeconds = 930,
                    completionCount = 55,
                    isFavorite = true
                ),
                VideoItem(
                    id = "v_short_2",
                    title = "Synthwave Neon Bass Drop",
                    artist = "Nasir Khan Visuals",
                    uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    durationSeconds = 15,
                    isShort = true,
                    resolution = "1080x1920",
                    playCount = 47,
                    totalWatchTimeSeconds = 680,
                    completionCount = 42,
                    isFavorite = true
                ),
                VideoItem(
                    id = "v_short_3",
                    title = "Tokyo Midnight Lo-Fi Beats Loop",
                    artist = "Nasir Khan Beats",
                    uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                    durationSeconds = 60,
                    isShort = true,
                    resolution = "1080x1920",
                    playCount = 33,
                    totalWatchTimeSeconds = 1800,
                    completionCount = 28,
                    isFavorite = false
                ),
                VideoItem(
                    id = "v_short_4",
                    title = "Turntable Pitch Shift Quick Tutorial",
                    artist = "Nasir Khan Masterclass",
                    uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
                    durationSeconds = 45,
                    isShort = true,
                    resolution = "1080x1920",
                    playCount = 28,
                    totalWatchTimeSeconds = 1120,
                    completionCount = 22,
                    isFavorite = true
                ),
                VideoItem(
                    id = "v_long_1",
                    title = "Music for Nasir - Full Visual Album Showcase",
                    artist = "Nasir Khan Production",
                    uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    durationSeconds = 596,
                    isShort = false,
                    resolution = "1920x1080",
                    playCount = 18,
                    totalWatchTimeSeconds = 7200,
                    completionCount = 12,
                    isFavorite = true
                ),
                VideoItem(
                    id = "v_long_2",
                    title = "Cyber Odyssey 2088 - Complete Studio Session",
                    artist = "Nasir Khan",
                    uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    durationSeconds = 653,
                    isShort = false,
                    resolution = "1920x1080",
                    playCount = 14,
                    totalWatchTimeSeconds = 5400,
                    completionCount = 8,
                    isFavorite = false
                ),
                VideoItem(
                    id = "v_long_3",
                    title = "Night Drive In Neo-Tokyo - 4K Synth Concert",
                    artist = "Nasir Khan & Friends",
                    uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    durationSeconds = 734,
                    isShort = false,
                    resolution = "1920x1080",
                    playCount = 21,
                    totalWatchTimeSeconds = 8800,
                    completionCount = 15,
                    isFavorite = true
                )
            )

            videoDao.insertVideos(sampleVideos)
        }
    }
}

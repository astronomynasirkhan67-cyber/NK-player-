package com.example.data.repository

import com.example.data.local.PlaylistDao
import com.example.data.local.SongDao
import com.example.data.model.Playlist
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow

class MusicRepository(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao
) {
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val mostPlayedSongs: Flow<List<Song>> = songDao.getMostPlayedSongs()
    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs()
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> {
        return playlistDao.getSongsForPlaylist(playlistId)
    }

    suspend fun incrementPlayCount(songId: String) {
        songDao.incrementPlayCount(songId)
    }

    suspend fun toggleFavorite(songId: String, currentFav: Boolean) {
        songDao.setFavorite(songId, !currentFav)
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
                    lyrics = "[00:00] Synth intro builds with energy\n[00:15] Glowing neon pulses through the night\n[00:30] Feel the rhythm taking flight\n[00:45] Music Nasir Khan on the stereo loud\n[01:00] Turntable spins above the crowd\n[01:15] Electric pulse in every vein\n[01:30] Dance until the morning rain\n[01:45] Pure sonic bliss and harmony\n[02:00] Nasir Khan signature melody"
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
                    lyrics = "[00:00] Soft vinyl crackle and warm Rhodes chords\n[00:22] Gentle raindrops tapping on the windowpane\n[00:44] Late night thoughts and steaming tea\n[01:06] Peaceful moments calm and free\n[01:28] Mellow melody soothing the mind\n[01:50] Sweet nostalgic feelings left behind"
                ),
                Song(
                    id = "s4",
                    title = "Cosmic Nebula Reverie",
                    artist = "Nasir Khan Soundscapes",
                    album = "Celestial Voyage",
                    durationSeconds = 245,
                    coverResName = "img_cover_cosmic",
                    genre = "Ambient / Space",
                    bpm = 65,
                    playCount = 23,
                    isFavorite = true,
                    lyrics = "[00:00] Drifting through the starry mist\n[00:30] Rings of Saturn, golden light\n[01:00] Deep interstellar echoes call\n[01:30] Floating where no shadows fall\n[02:00] Ethereal frequencies vibrate far\n[02:25] Reaching for the distant star"
                ),
                Song(
                    id = "s5",
                    title = "Acoustic Sunset Horizon",
                    artist = "Nasir Khan Acoustic Trio",
                    album = "Golden Hour Sessions",
                    durationSeconds = 175,
                    coverResName = "img_cover_lofi",
                    genre = "Acoustic / Indie",
                    bpm = 95,
                    playCount = 18,
                    isFavorite = false,
                    lyrics = "[00:00] Fingerpicked acoustic guitar resonance\n[00:20] Warm breeze across the open shore\n[00:40] Golden sunlight fading slow\n[01:00] Memories that gently glow\n[01:20] Harmonic strings in sweet accord\n[01:40] Peaceful rhythm, quiet reward"
                ),
                Song(
                    id = "s6",
                    title = "Funk Groove Dynamite",
                    artist = "Nasir Khan Funk Collective",
                    album = "Retro Disco Fever",
                    durationSeconds = 205,
                    coverResName = "img_cover_cyber",
                    genre = "Nu-Funk / Disco",
                    bpm = 118,
                    playCount = 15,
                    isFavorite = false,
                    lyrics = "[00:00] Slap bass groove kicks in tight\n[00:18] Funky guitar skanks ignite\n[00:36] Get up on your dancing feet\n[00:54] Lock into this funky beat\n[01:12] Brass sections tearing through\n[01:30] Nasir's groove made just for you"
                ),
                Song(
                    id = "s7",
                    title = "Midnight Jazz Lounge",
                    artist = "Nasir Khan Quintet",
                    album = "Blue Velvet Suite",
                    durationSeconds = 220,
                    coverResName = "img_cover_cosmic",
                    genre = "Smooth Jazz",
                    bpm = 78,
                    playCount = 12,
                    isFavorite = false,
                    lyrics = "[00:00] Silky saxophone enters softly\n[00:25] Brushes swirling on the snare\n[00:50] Candlelight reflections shimmer\n[01:15] Sophisticated midnight air\n[01:40] Minor seventh chords unfold\n[02:05] Timeless stories gently told"
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

            // Populate some initial playlist songs
            playlistDao.addSongToPlaylist(PlaylistSongCrossRef("pl_chill", "s2"))
            playlistDao.addSongToPlaylist(PlaylistSongCrossRef("pl_chill", "s3"))
            playlistDao.addSongToPlaylist(PlaylistSongCrossRef("pl_chill", "s4"))
            playlistDao.addSongToPlaylist(PlaylistSongCrossRef("pl_chill", "s5"))

            playlistDao.addSongToPlaylist(PlaylistSongCrossRef("pl_energy", "s1"))
            playlistDao.addSongToPlaylist(PlaylistSongCrossRef("pl_energy", "s6"))
            playlistDao.addSongToPlaylist(PlaylistSongCrossRef("pl_energy", "s8"))
        }
    }
}

package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val coverResName: String,
    val genre: String,
    val bpm: Int = 120,
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,
    val firstPlayedTimestamp: Long = 0L,
    val totalListeningTimeSeconds: Long = 0L,
    val completionCount: Int = 0,
    val uri: String = "",
    val lyrics: String = ""
)

@Entity(tableName = "videos")
data class VideoItem(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val uri: String,
    val durationSeconds: Int,
    val isShort: Boolean = false,
    val resolution: String = "1080p",
    val playCount: Int = 0,
    val lastWatchedTimestamp: Long = 0L,
    val firstPlayedTimestamp: Long = 0L,
    val totalWatchTimeSeconds: Long = 0L,
    val completionCount: Int = 0,
    val isFavorite: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val mediaId: String,
    val mediaType: String, // "MUSIC" or "VIDEO"
    val title: String,
    val subtitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationPlayedSeconds: Int = 0
)

data class OverallStatistics(
    val totalMusicPlays: Int = 0,
    val totalListeningTimeSeconds: Long = 0L,
    val totalMusicCompletions: Int = 0,
    val totalVideoPlays: Int = 0,
    val totalVideoWatchTimeSeconds: Long = 0L,
    val totalVideoCompletions: Int = 0,
    val totalShortsCount: Int = 0,
    val totalLongVideosCount: Int = 0
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val isSystem: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: String,
    val songId: String,
    val addedAt: Long = System.currentTimeMillis()
)

enum class AppTheme(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val accentHex: Long,
    val backgroundHex: Long,
    val surfaceHex: Long,
    val discLabelColorHex: Long
) {
    IMMERSIVE_UI(
        id = "immersive_ui",
        displayName = "Immersive UI",
        subtitle = "Deep Obsidian & Radiant Indigo Glow",
        primaryHex = 0xFF818CF8,
        secondaryHex = 0xFFC084FC,
        accentHex = 0xFF38BDF8,
        backgroundHex = 0xFF0A0B0E,
        surfaceHex = 0xFF141622,
        discLabelColorHex = 0xFF6366F1
    ),
    CYBER_NEON(
        id = "cyber_neon",
        displayName = "Cyber Neon",
        subtitle = "Futuristic Cyan & Magenta",
        primaryHex = 0xFF00F0FF,
        secondaryHex = 0xFFFF007F,
        accentHex = 0xFF7000FF,
        backgroundHex = 0xFF0A0718,
        surfaceHex = 0xFF171132,
        discLabelColorHex = 0xFF9900FF
    ),
    VINYL_AMBER(
        id = "vinyl_amber",
        displayName = "Vinyl Retro Amber",
        subtitle = "Warm Mahogany & Honey Gold",
        primaryHex = 0xFFFFB300,
        secondaryHex = 0xFFFF7043,
        accentHex = 0xFFFFCA28,
        backgroundHex = 0xFF140D07,
        surfaceHex = 0xFF24170E,
        discLabelColorHex = 0xFFD84315
    ),
    COSMIC_PURPLE(
        id = "cosmic_purple",
        displayName = "Cosmic Starlight",
        subtitle = "Deep Indigo & Radiant Violet",
        primaryHex = 0xFFB388FF,
        secondaryHex = 0xFF7C4DFF,
        accentHex = 0xFF64FFDA,
        backgroundHex = 0xFF070614,
        surfaceHex = 0xFF14122B,
        discLabelColorHex = 0xFF651FFF
    ),
    MIDNIGHT_EMERALD(
        id = "midnight_emerald",
        displayName = "Aurora Emerald",
        subtitle = "Luminous Mint & Jade Forest",
        primaryHex = 0xFF00E676,
        secondaryHex = 0xFF1DE9B6,
        accentHex = 0xFF76FF03,
        backgroundHex = 0xFF04120C,
        surfaceHex = 0xFF0B241A,
        discLabelColorHex = 0xFF00BFA5
    ),
    SOLAR_SUNSET(
        id = "solar_sunset",
        displayName = "Solar Rose",
        subtitle = "Coral Glow & Crimson Fire",
        primaryHex = 0xFFFF5252,
        secondaryHex = 0xFFFF4081,
        accentHex = 0xFFFFAB40,
        backgroundHex = 0xFF14070B,
        surfaceHex = 0xFF2A0F17,
        discLabelColorHex = 0xFFC2185B
    ),
    FROSTED_CRYSTAL(
        id = "frosted_crystal",
        displayName = "Ice Crystal",
        subtitle = "Subzero Glacier & Diamond White",
        primaryHex = 0xFF80D8FF,
        secondaryHex = 0xFF40C4FF,
        accentHex = 0xFFE0F7FA,
        backgroundHex = 0xFF08131E,
        surfaceHex = 0xFF112338,
        discLabelColorHex = 0xFF0091EA
    )
}

data class EqualizerSettings(
    val bassBoost: Float = 0.5f,
    val treble: Float = 0.5f,
    val vocalClarity: Float = 0.5f,
    val surround3D: Float = 0.4f,
    val vinylCrackle: Boolean = false,
    val presetName: String = "Balanced"
)

package com.example.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.data.model.Song
import com.example.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalMediaScanner(private val context: Context) {

    private val supportedAudioMimeTypes = setOf(
        "audio/mpeg",
        "audio/mp3",
        "audio/mp4",
        "audio/x-m4a",
        "audio/aac",
        "audio/wav",
        "audio/x-wav",
        "audio/ogg",
        "audio/flac",
        "audio/x-flac",
        "audio/opus",
        "audio/3gpp",
        "audio/amr",
        "audio/midi",
        "audio/x-matroska"
    )

    private val supportedAudioExtensions = setOf(
        "mp3", "m4a", "aac", "wav", "ogg", "flac", "opus", "wma", "3gp", "amr"
    )

    private val supportedVideoMimeTypes = setOf(
        "video/mp4",
        "video/mkv",
        "video/x-matroska",
        "video/avi",
        "video/x-msvideo",
        "video/quicktime",
        "video/webm",
        "video/3gpp",
        "video/mp2ts",
        "video/x-flv"
    )

    private val supportedVideoExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "webm", "3gp", "ts", "m4v", "flv"
    )

    suspend fun scanLocalAudio(): List<Song> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE
        )

        // Include music files or any audio mime type with duration >= 1 second
        val selection = "${MediaStore.Audio.Media.DURATION} >= 1000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val displayNameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val mimeTypeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    try {
                        if (idCol == -1) continue
                        val id = cursor.getLong(idCol)
                        val displayName = if (displayNameCol != -1) cursor.getString(displayNameCol) ?: "" else ""
                        val mimeType = if (mimeTypeCol != -1) cursor.getString(mimeTypeCol) ?: "" else ""

                        val ext = displayName.substringAfterLast('.', "").lowercase()
                        val isAudio = mimeType.startsWith("audio/") ||
                                mimeType in supportedAudioMimeTypes ||
                                ext in supportedAudioExtensions

                        if (!isAudio && mimeType.isNotBlank() && !displayName.endsWith(".mp3", ignoreCase = true)) {
                            continue
                        }

                        val rawTitle = if (titleCol != -1) cursor.getString(titleCol) else null
                        val rawArtist = if (artistCol != -1) cursor.getString(artistCol) else null
                        val rawAlbum = if (albumCol != -1) cursor.getString(albumCol) else null
                        val albumId = if (albumIdCol != -1) cursor.getLong(albumIdCol) else -1L
                        val durationMs = if (durationCol != -1) cursor.getInt(durationCol) else 0

                        val durationSec = (durationMs / 1000).coerceAtLeast(1)

                        val cleanTitle = when {
                            !rawTitle.isNullOrBlank() && !rawTitle.equals("<unknown>", ignoreCase = true) -> rawTitle.trim()
                            displayName.isNotBlank() -> displayName.substringBeforeLast('.')
                            else -> "Track $id"
                        }

                        val cleanArtist = when {
                            !rawArtist.isNullOrBlank() && !rawArtist.equals("<unknown>", ignoreCase = true) -> rawArtist.trim()
                            else -> "Unknown Artist"
                        }

                        val cleanAlbum = when {
                            !rawAlbum.isNullOrBlank() && !rawAlbum.equals("<unknown>", ignoreCase = true) -> rawAlbum.trim()
                            else -> "Device Storage"
                        }

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()

                        val albumArtUri = if (albumId > 0) {
                            ContentUris.withAppendedId(
                                Uri.parse("content://media/external/audio/albumart"),
                                albumId
                            ).toString()
                        } else {
                            ""
                        }

                        // Cycle decorative visual cover for turntable styling
                        val coverResName = when ((id % 3).toInt()) {
                            0 -> "img_cover_cyber"
                            1 -> "img_cover_lofi"
                            else -> "img_cover_cosmic"
                        }

                        audioList.add(
                            Song(
                                id = "local_audio_$id",
                                title = cleanTitle,
                                artist = cleanArtist,
                                album = cleanAlbum,
                                durationSeconds = durationSec,
                                coverResName = coverResName,
                                albumArtUri = albumArtUri,
                                genre = "Local Audio",
                                bpm = 120,
                                playCount = 0,
                                isFavorite = false,
                                uri = contentUri,
                                lyrics = "Device Local Audio File\nTitle: $cleanTitle\nArtist: $cleanArtist\nAlbum: $cleanAlbum"
                            )
                        )
                    } catch (rowEx: Exception) {
                        Log.w("LocalMediaScanner", "Error reading audio row, skipping", rowEx)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LocalMediaScanner", "Error querying audio MediaStore", e)
        }

        audioList
    }

    suspend fun scanLocalVideos(shortsThresholdSeconds: Int = 90): List<VideoItem> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.ARTIST,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Video.Media.ARTIST)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val displayNameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val mimeTypeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    try {
                        if (idCol == -1) continue
                        val id = cursor.getLong(idCol)
                        val displayName = if (displayNameCol != -1) cursor.getString(displayNameCol) ?: "" else ""
                        val mimeType = if (mimeTypeCol != -1) cursor.getString(mimeTypeCol) ?: "" else ""

                        val ext = displayName.substringAfterLast('.', "").lowercase()
                        val isVideo = mimeType.startsWith("video/") ||
                                mimeType in supportedVideoMimeTypes ||
                                ext in supportedVideoExtensions

                        if (!isVideo && mimeType.isNotBlank()) {
                            continue
                        }

                        val rawTitle = if (titleCol != -1) cursor.getString(titleCol) else null
                        val rawArtist = if (artistCol != -1) cursor.getString(artistCol) else null
                        val durationMs = if (durationCol != -1) cursor.getInt(durationCol) else 0
                        val durationSec = (durationMs / 1000).coerceAtLeast(1)
                        val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                        val height = if (heightCol != -1) cursor.getInt(heightCol) else 0
                        val dateAddedSec = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) else System.currentTimeMillis() / 1000

                        val cleanTitle = when {
                            !rawTitle.isNullOrBlank() && !rawTitle.equals("<unknown>", ignoreCase = true) -> rawTitle.trim()
                            displayName.isNotBlank() -> displayName.substringBeforeLast('.')
                            else -> "Video $id"
                        }

                        val cleanArtist = when {
                            !rawArtist.isNullOrBlank() && !rawArtist.equals("<unknown>", ignoreCase = true) -> rawArtist.trim()
                            else -> "Device Video"
                        }

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()

                        // Shorts up to 90 seconds (1m 30s)
                        val isShort = durationSec in 1..shortsThresholdSeconds

                        videoList.add(
                            VideoItem(
                                id = "local_video_$id",
                                title = cleanTitle,
                                artist = cleanArtist,
                                uri = contentUri,
                                durationSeconds = durationSec,
                                isShort = isShort,
                                resolution = if (width > 0 && height > 0) "${width}x${height}" else "HD",
                                playCount = 0,
                                lastWatchedTimestamp = 0L,
                                firstPlayedTimestamp = 0L,
                                totalWatchTimeSeconds = 0L,
                                completionCount = 0,
                                isFavorite = false,
                                addedAt = dateAddedSec * 1000
                            )
                        )
                    } catch (rowEx: Exception) {
                        Log.w("LocalMediaScanner", "Error reading video row, skipping", rowEx)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LocalMediaScanner", "Error querying video MediaStore", e)
        }

        videoList
    }
}

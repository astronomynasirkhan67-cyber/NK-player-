package com.example.data.local

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.data.model.Song
import com.example.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalMediaScanner(private val context: Context) {

    suspend fun scanLocalAudio(): List<Song> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown Track"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Local Music"
                    val durationMs = cursor.getInt(durationColumn)
                    val durationSec = (durationMs / 1000).coerceAtLeast(1)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    audioList.add(
                        Song(
                            id = "local_audio_$id",
                            title = title,
                            artist = if (artist.contains("<unknown>", ignoreCase = true)) "Nasir Khan (Local)" else artist,
                            album = album,
                            durationSeconds = durationSec,
                            coverResName = "img_cover_cyber",
                            genre = "Local Audio",
                            bpm = 120,
                            playCount = 0,
                            isFavorite = false,
                            uri = contentUri,
                            lyrics = "Local Audio file from Device Storage\nTitle: $title\nArtist: $artist"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("LocalMediaScanner", "Error scanning audio from MediaStore", e)
        }

        audioList
    }

    suspend fun scanLocalVideos(shortsThresholdSeconds: Int = 60): List<VideoItem> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.ARTIST,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
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
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Video $id"
                    val artist = cursor.getString(artistColumn) ?: "Local Video"
                    val durationMs = cursor.getInt(durationColumn)
                    val durationSec = (durationMs / 1000).coerceAtLeast(1)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val isShort = durationSec <= shortsThresholdSeconds

                    videoList.add(
                        VideoItem(
                            id = "local_video_$id",
                            title = title,
                            artist = if (artist.contains("<unknown>", ignoreCase = true)) "Nasir Khan Visuals" else artist,
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
                            addedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("LocalMediaScanner", "Error scanning videos from MediaStore", e)
        }

        videoList
    }
}

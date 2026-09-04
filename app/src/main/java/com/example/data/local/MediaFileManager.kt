package com.example.data.local

import android.content.ClipData
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.Song
import com.example.data.model.VideoItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

sealed class MediaTarget {
    data class SongMedia(val song: Song) : MediaTarget()
    data class VideoMedia(val video: VideoItem) : MediaTarget()

    val id: String get() = when (this) {
        is SongMedia -> song.id
        is VideoMedia -> video.id
    }

    val title: String get() = when (this) {
        is SongMedia -> song.title
        is VideoMedia -> video.title
    }

    val artist: String get() = when (this) {
        is SongMedia -> song.artist
        is VideoMedia -> video.artist
    }

    val uriString: String get() = when (this) {
        is SongMedia -> song.uri
        is VideoMedia -> video.uri
    }

    val durationSeconds: Int get() = when (this) {
        is SongMedia -> song.durationSeconds
        is VideoMedia -> video.durationSeconds
    }

    val isVideo: Boolean get() = this is VideoMedia
    val isShort: Boolean get() = (this as? VideoMedia)?.video?.isShort == true
}

data class MediaFileInfo(
    val file: File?,
    val filePath: String?,
    val fileName: String,
    val baseName: String,
    val extension: String,
    val fileSize: Long,
    val uri: Uri,
    val parentFolder: String?
)

data class FolderDestination(
    val name: String,
    val directory: File,
    val description: String,
    val iconType: String // "MUSIC", "MOVIES", "DOWNLOAD", "FOLDER"
)

data class MediaRenameResult(
    val newTitle: String,
    val newFileName: String,
    val newUri: String
)

data class MediaMoveResult(
    val targetFile: File,
    val newUri: String,
    val destinationFolderName: String
)

object MediaFileManager {

    private const val TAG = "MediaFileManager"
    private val INVALID_FILENAME_CHARS = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|', '\u0000')

    fun resolveFileInfo(context: Context, item: MediaTarget): MediaFileInfo {
        val uri = Uri.parse(item.uriString)
        var fileName: String? = null
        var filePath: String? = null
        var fileSize: Long = 0L

        if (uri.scheme == "file") {
            filePath = uri.path
            fileName = filePath?.let { File(it).name }
            fileSize = filePath?.let { File(it).length() } ?: 0L
        } else if (uri.scheme == "content") {
            try {
                val projection = arrayOf(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.DATA,
                    MediaStore.MediaColumns.SIZE
                )
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                        val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                        val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)

                        if (nameCol != -1) fileName = cursor.getString(nameCol)
                        if (dataCol != -1) filePath = cursor.getString(dataCol)
                        if (sizeCol != -1) fileSize = cursor.getLong(sizeCol)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error resolving MediaStore info for $uri", e)
            }
        }

        // Try direct file path if resolved
        val file = filePath?.let { File(it) }?.takeIf { it.exists() }
        val defaultExt = if (item.isVideo) "mp4" else "mp3"

        val resolvedFileName = when {
            !fileName.isNullOrBlank() -> fileName!!
            file != null -> file.name
            else -> {
                val safeTitle = item.title.replace(Regex("[/\\\\:*?\"<>|]"), "_")
                "$safeTitle.$defaultExt"
            }
        }

        val baseName = if (resolvedFileName.contains('.')) {
            resolvedFileName.substringBeforeLast('.')
        } else {
            item.title
        }

        val extension = if (resolvedFileName.contains('.')) {
            resolvedFileName.substringAfterLast('.').lowercase()
        } else {
            defaultExt
        }

        val parentFolder = file?.parentFile?.name ?: file?.parent

        return MediaFileInfo(
            file = file,
            filePath = filePath,
            fileName = resolvedFileName,
            baseName = baseName,
            extension = extension,
            fileSize = fileSize,
            uri = uri,
            parentFolder = parentFolder
        )
    }

    fun validateFileName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return "Name cannot be empty"
        }
        for (char in INVALID_FILENAME_CHARS) {
            if (trimmed.contains(char)) {
                return "Name cannot contain special character: $char"
            }
        }
        if (trimmed.startsWith(".") || trimmed.endsWith(".")) {
            return "Name cannot start or end with a dot"
        }
        return null
    }

    fun renameMedia(context: Context, item: MediaTarget, newBaseName: String): Result<MediaRenameResult> {
        val trimmedBaseName = newBaseName.trim()
        val validationError = validateFileName(trimmedBaseName)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        val info = resolveFileInfo(context, item)
        val newFileName = "$trimmedBaseName.${info.extension}"

        // Check if destination file already exists in same folder
        if (info.file != null && info.file.parentFile != null) {
            val targetFile = File(info.file.parentFile, newFileName)
            if (targetFile.exists() && targetFile.canonicalPath != info.file.canonicalPath) {
                return Result.failure(IllegalStateException("A file with name '$newFileName' already exists in this folder"))
            }

            try {
                val success = info.file.renameTo(targetFile)
                if (success) {
                    // Update MediaStore
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, newFileName)
                        put(MediaStore.MediaColumns.TITLE, trimmedBaseName)
                        put(MediaStore.MediaColumns.DATA, targetFile.absolutePath)
                    }
                    try {
                        context.contentResolver.update(info.uri, values, null, null)
                    } catch (_: Exception) {}

                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath, info.file.absolutePath),
                        null,
                        null
                    )

                    return Result.success(
                        MediaRenameResult(
                            newTitle = trimmedBaseName,
                            newFileName = newFileName,
                            newUri = Uri.fromFile(targetFile).toString()
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct file rename failed, attempting MediaStore update", e)
            }
        }

        // Fallback or MediaStore Scoped Storage rename
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newFileName)
                put(MediaStore.MediaColumns.TITLE, trimmedBaseName)
            }
            context.contentResolver.update(info.uri, values, null, null)

            return Result.success(
                MediaRenameResult(
                    newTitle = trimmedBaseName,
                    newFileName = newFileName,
                    newUri = info.uri.toString()
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Rename failed", e)
            return Result.failure(Exception("Failed to rename media: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    fun shareMedia(context: Context, item: MediaTarget) {
        val info = resolveFileInfo(context, item)
        val shareUri: Uri = try {
            if (info.file != null && info.file.exists()) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", info.file)
            } else if (info.uri.scheme == "content") {
                info.uri
            } else {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(item.uriString))
            }
        } catch (e: Exception) {
            Log.w(TAG, "FileProvider URI generation failed, falling back to original URI", e)
            info.uri
        }

        val mimeType = if (item.isVideo) "video/*" else "audio/*"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_SUBJECT, item.title)
            putExtra(Intent.EXTRA_TEXT, "Shared from Music Nasir Khan: ${item.title}")
            clipData = ClipData.newUri(context.contentResolver, item.title, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share ${item.title}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }

    fun getAvailableFolders(context: Context): List<FolderDestination> {
        val folders = mutableListOf<FolderDestination>()

        // 1. Primary Public Directories
        val musicPublic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val moviesPublic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val downloadsPublic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dcimPublic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val podcastsPublic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS)

        fun addIfAccessible(dir: File, name: String, desc: String, icon: String) {
            try {
                if (!dir.exists()) dir.mkdirs()
                if (dir.exists() && dir.isDirectory) {
                    folders.add(FolderDestination(name, dir, desc, icon))
                    // Also scan subdirectories inside this folder
                    dir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.take(6)?.forEach { sub ->
                        folders.add(
                            FolderDestination(
                                name = "$name / ${sub.name}",
                                directory = sub,
                                description = sub.absolutePath,
                                iconType = "FOLDER"
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        addIfAccessible(musicPublic, "Music", "System Music Library", "MUSIC")
        addIfAccessible(moviesPublic, "Movies", "System Videos & Movies Library", "MOVIES")
        addIfAccessible(downloadsPublic, "Downloads", "Device Downloads Folder", "DOWNLOAD")
        addIfAccessible(dcimPublic, "DCIM", "Camera & Captured Media", "MOVIES")
        addIfAccessible(podcastsPublic, "Podcasts", "Audio Podcasts & Talks", "MUSIC")

        // 2. App-specific storage (always 100% writable on all Android versions)
        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.let { appMusic ->
            if (!appMusic.exists()) appMusic.mkdirs()
            folders.add(FolderDestination("App Music Vault", appMusic, "Nasir Khan Music Vault", "MUSIC"))
        }
        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.let { appMovies ->
            if (!appMovies.exists()) appMovies.mkdirs()
            folders.add(FolderDestination("App Videos Vault", appMovies, "Nasir Khan Videos Vault", "MOVIES"))
        }

        return folders
    }

    fun createNewFolder(parentDir: File, folderName: String): Result<File> {
        val trimmed = folderName.trim()
        val validation = validateFileName(trimmed)
        if (validation != null) {
            return Result.failure(IllegalArgumentException(validation))
        }

        val newDir = File(parentDir, trimmed)
        if (newDir.exists()) {
            return Result.failure(IllegalStateException("Folder '$trimmed' already exists"))
        }

        val created = newDir.mkdirs()
        return if (created) {
            Result.success(newDir)
        } else {
            Result.failure(Exception("Could not create folder in ${parentDir.name}"))
        }
    }

    fun moveMedia(context: Context, item: MediaTarget, destinationDir: File): Result<MediaMoveResult> {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }
        if (!destinationDir.isDirectory) {
            return Result.failure(IllegalArgumentException("Target is not a valid directory"))
        }

        val info = resolveFileInfo(context, item)
        var targetFile = File(destinationDir, info.fileName)

        // Prevent moving to the exact same folder
        if (info.file != null && info.file.parentFile?.canonicalPath == destinationDir.canonicalPath) {
            return Result.failure(IllegalStateException("File is already in '${destinationDir.name}'"))
        }

        // Handle conflict if a file with same name exists at destination
        if (targetFile.exists()) {
            var counter = 1
            while (targetFile.exists()) {
                targetFile = File(destinationDir, "${info.baseName}_$counter.${info.extension}")
                counter++
            }
        }

        // Try direct file copy & delete if source file exists
        if (info.file != null && info.file.exists()) {
            try {
                FileInputStream(info.file).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                info.file.delete()

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath, info.file.absolutePath),
                    null,
                    null
                )

                return Result.success(
                    MediaMoveResult(
                        targetFile = targetFile,
                        newUri = Uri.fromFile(targetFile).toString(),
                        destinationFolderName = destinationDir.name
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Direct file copy failed, trying ContentResolver move", e)
            }
        }

        // ContentResolver relative path update fallback
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePath = "${destinationDir.name}/"
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                }
                context.contentResolver.update(info.uri, values, null, null)
                return Result.success(
                    MediaMoveResult(
                        targetFile = targetFile,
                        newUri = info.uri.toString(),
                        destinationFolderName = destinationDir.name
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "ContentResolver move failed", e)
        }

        return Result.failure(Exception("Failed to move media file to ${destinationDir.name}"))
    }

    fun deleteMedia(context: Context, item: MediaTarget): Result<Boolean> {
        val info = resolveFileInfo(context, item)
        var fileDeleted = false
        var contentDeleted = false

        // 1. Delete physical file if available
        if (info.file != null && info.file.exists()) {
            try {
                fileDeleted = info.file.delete()
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(info.file.absolutePath),
                    null,
                    null
                )
            } catch (e: Exception) {
                Log.w(TAG, "File delete failed", e)
            }
        }

        // 2. Delete from MediaStore ContentResolver
        try {
            val rows = context.contentResolver.delete(info.uri, null, null)
            contentDeleted = rows > 0
        } catch (e: Exception) {
            Log.w(TAG, "ContentResolver delete failed", e)
        }

        return if (fileDeleted || contentDeleted || info.file == null) {
            Result.success(true)
        } else {
            Result.failure(Exception("Unable to delete media file from storage"))
        }
    }
}

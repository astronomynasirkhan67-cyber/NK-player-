package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MediaDeleteResult
import com.example.data.local.MediaFileManager
import com.example.data.local.MediaTarget
import com.example.data.model.Song
import com.example.data.model.VideoItem
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Music for Nasir", appName)
  }

  @Test
  fun `test physical media deletion on local file`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testFile = File(context.filesDir, "ai_studio_code.ts")
    testFile.writeText("sample video content")
    assertTrue(testFile.exists())

    val video = VideoItem(
        id = "video_9999",
        title = "ai_studio_code",
        artist = "Nasir Khan",
        uri = testFile.absolutePath,
        durationSeconds = 30,
        isShort = true
    )
    val target = MediaTarget.VideoMedia(video)

    val result = MediaFileManager.deleteMedia(context, target)
    assertTrue("Deletion should succeed for local file", result is MediaDeleteResult.Success)
    assertFalse("Physical file should be deleted from storage", testFile.exists())
  }

  @Test
  fun `test delete non existent file fails safely without fake success`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val nonExistentFile = File(context.filesDir, "ghost_song.mp3")
    assertFalse(nonExistentFile.exists())

    val song = Song(
        id = "song_8888",
        title = "Ghost Song",
        artist = "Unknown",
        album = "None",
        durationSeconds = 120,
        coverResName = "cover_placeholder",
        genre = "Ambient",
        uri = nonExistentFile.absolutePath
    )
    val target = MediaTarget.SongMedia(song)

    val result = MediaFileManager.deleteMedia(context, target)
    assertTrue("Should return Failure when file is missing from storage", result is MediaDeleteResult.Failure)
  }
}

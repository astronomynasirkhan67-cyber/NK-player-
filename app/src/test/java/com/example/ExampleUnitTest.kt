package com.example

import androidx.compose.ui.unit.dp
import com.example.data.model.VideoItem
import com.example.ui.components.VideoDimensionHelper
import com.example.ui.viewmodel.NavigationTab
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testNavigationTabsContainShortsAndVideos() {
    val tabs = NavigationTab.values().map { it.name }
    assertEquals(6, tabs.size)
    assertTrue("Should include NOW_PLAYING tab (Player)", tabs.contains("NOW_PLAYING"))
    assertTrue("Should include PLAYLISTS tab (Music)", tabs.contains("PLAYLISTS"))
    assertTrue("Should include SHORTS tab (Shorts)", tabs.contains("SHORTS"))
    assertTrue("Should include VIDEOS tab (Videos)", tabs.contains("VIDEOS"))
    assertTrue("Should include STATS tab (Stats)", tabs.contains("STATS"))
    assertTrue("Should include THEMES_LAB tab (Themes)", tabs.contains("THEMES_LAB"))
  }

  @Test
  fun testShortsDurationFilteringStrictly90SecondsOrLess() {
    val testVideos = listOf(
      VideoItem(id = "1", title = "15s Short", uri = "uri1", durationSeconds = 15, isShort = true),
      VideoItem(id = "2", title = "30s Short", uri = "uri2", durationSeconds = 30, isShort = true),
      VideoItem(id = "3", title = "60s Short", uri = "uri3", durationSeconds = 60, isShort = true),
      VideoItem(id = "4", title = "90s Short (1m30s)", uri = "uri4", durationSeconds = 90, isShort = true),
      VideoItem(id = "5", title = "91s Long Video", uri = "uri5", durationSeconds = 91, isShort = false),
      VideoItem(id = "6", title = "120s Movie", uri = "uri6", durationSeconds = 120, isShort = false),
      VideoItem(id = "7", title = "0s Corrupt", uri = "uri7", durationSeconds = 0, isShort = false)
    )

    val threshold = 90
    val shorts = testVideos.filter { it.durationSeconds in 1..threshold }
    val longVideos = testVideos.filter { it.durationSeconds > threshold }

    assertEquals(4, shorts.size)
    assertTrue(shorts.all { it.durationSeconds <= 90 })
    assertTrue(shorts.any { it.title == "90s Short (1m30s)" })
    assertFalse(shorts.any { it.title == "91s Long Video" })
    assertFalse(shorts.any { it.title == "120s Movie" })

    assertEquals(2, longVideos.size)
    assertTrue(longVideos.all { it.durationSeconds > 90 })
    assertFalse(longVideos.any { it.title == "90s Short (1m30s)" })
  }

  @Test
  fun testVideoDimensionHelperParseRatio() {
    // 16:9 landscape
    val landscapeRatio = VideoDimensionHelper.parseRatio("1920x1080")
    assertNotNull(landscapeRatio)
    assertEquals(1920f / 1080f, landscapeRatio!!, 0.001f)

    // 9:16 portrait
    val portraitRatio = VideoDimensionHelper.parseRatio("1080x1920")
    assertNotNull(portraitRatio)
    assertEquals(1080f / 1920f, portraitRatio!!, 0.001f)

    // 1:1 square
    val squareRatio = VideoDimensionHelper.parseRatio("1080x1080")
    assertNotNull(squareRatio)
    assertEquals(1.0f, squareRatio!!, 0.001f)

    // Invalid / null
    assertNull(VideoDimensionHelper.parseRatio(null))
    assertNull(VideoDimensionHelper.parseRatio("HD"))
    assertNull(VideoDimensionHelper.parseRatio(""))
  }

  @Test
  fun testAspectFitSizeCalculationsPreserveProportions() {
    // Container is portrait screen: 400.dp x 800.dp (ratio = 0.5)
    val containerW = 400.dp
    val containerH = 800.dp

    // Portrait video: 9:16 = 0.5625 (wider than 0.5 container)
    // Should fit container width (400.dp) and scale height without exceeding container
    val portraitRatio = 9f / 16f // 0.5625
    val (pFitW, pFitH) = VideoDimensionHelper.calculateAspectFitSize(portraitRatio, containerW, containerH)
    assertEquals(400f, pFitW.value, 0.1f)
    assertEquals(400f / 0.5625f, pFitH.value, 0.1f) // ~711.1dp <= 800dp (pillarbox top/bottom, NO stretch!)

    // Landscape video: 16:9 = 1.777 in same portrait screen
    val landscapeRatio = 16f / 9f
    val (lFitW, lFitH) = VideoDimensionHelper.calculateAspectFitSize(landscapeRatio, containerW, containerH)
    assertEquals(400f, lFitW.value, 0.1f)
    assertEquals(400f / (16f / 9f), lFitH.value, 0.1f) // 225dp <= 800dp (letterbox top/bottom, NO stretch!)

    // Container is landscape screen: 800.dp x 400.dp (ratio = 2.0)
    val landscapeContainerW = 800.dp
    val landscapeContainerH = 400.dp

    // Portrait video 9:16 = 0.5625 in landscape screen
    val (pInLandW, pInLandH) = VideoDimensionHelper.calculateAspectFitSize(portraitRatio, landscapeContainerW, landscapeContainerH)
    assertEquals(400f, pInLandH.value, 0.1f)
    assertEquals(400f * 0.5625f, pInLandW.value, 0.1f) // 225dp <= 800dp (pillarbox left/right, NO stretch to landscape!)
  }
}

package com.example

import com.example.data.model.VideoItem
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
    assertTrue("Should include SHORTS tab", tabs.contains("SHORTS"))
    assertTrue("Should include VIDEOS tab", tabs.contains("VIDEOS"))
    assertTrue("Should include NOW_PLAYING tab", tabs.contains("NOW_PLAYING"))
    assertTrue("Should include PLAYLISTS tab", tabs.contains("PLAYLISTS"))
  }

  @Test
  fun testShortsDurationFilteringStrictly60SecondsOrLess() {
    val testVideos = listOf(
      VideoItem(id = "1", title = "30s Short", uri = "uri1", durationSeconds = 30, isShort = true),
      VideoItem(id = "2", title = "45s Short", uri = "uri2", durationSeconds = 45, isShort = true),
      VideoItem(id = "3", title = "60s Short", uri = "uri3", durationSeconds = 60, isShort = true),
      VideoItem(id = "4", title = "61s Long", uri = "uri4", durationSeconds = 61, isShort = false),
      VideoItem(id = "5", title = "120s Movie", uri = "uri5", durationSeconds = 120, isShort = false),
      VideoItem(id = "6", title = "0s Corrupt", uri = "uri6", durationSeconds = 0, isShort = false)
    )

    val shorts = testVideos.filter { it.durationSeconds in 1..60 }
    val longVideos = testVideos.filter { it.durationSeconds > 60 }

    assertEquals(3, shorts.size)
    assertTrue(shorts.all { it.durationSeconds <= 60 })
    assertFalse(shorts.any { it.title == "61s Long" })
    assertFalse(shorts.any { it.title == "120s Movie" })

    assertEquals(2, longVideos.size)
    assertTrue(longVideos.all { it.durationSeconds > 60 })
    assertFalse(longVideos.any { it.title == "60s Short" })
  }
}


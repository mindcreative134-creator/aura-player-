package com.cmstudio.apnaplayer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerBridgeTest {

  @Test
  fun testPropertyMapDefaults() {
    // Boolean properties
    assertFalse(PlayerBridge.propBoolean["pause"].value)
    assertFalse(PlayerBridge.propBoolean["paused-for-cache"].value)

    // Int properties
    assertEquals(100, PlayerBridge.propInt["volume"].value)
    assertEquals(1, PlayerBridge.propInt["aid"].value)
    assertEquals(1, PlayerBridge.propInt["sid"].value)

    // Float/Double properties
    assertEquals(1.0f, PlayerBridge.propFloat["speed"].value, 0.001f)
    assertEquals(1.0, PlayerBridge.propDouble["speed"].value, 0.001)

    // String properties
    assertEquals("mediacodec", PlayerBridge.propString["hwdec"].value)
  }

  @Test
  fun testSetAndGetProperties() {
    PlayerBridge.propInt["volume"].value = 85
    assertEquals(85, PlayerBridge.propInt["volume"].value)

    PlayerBridge.propFloat["speed"].value = 1.75f
    assertEquals(1.75f, PlayerBridge.propFloat["speed"].value, 0.001f)

    PlayerBridge.propDouble["time-pos"].value = 124.5
    assertEquals(124.5, PlayerBridge.propDouble["time-pos"].value, 0.001)

    PlayerBridge.propBoolean["pause"].value = true
    assertTrue(PlayerBridge.propBoolean["pause"].value)
  }

  @Test
  fun testPercentSeekingFormula() {
    val durationSec = 7200.0 // 2 hours
    val percentTarget = 100.0 // seeking to 100%
    val calculatedSec = (percentTarget / 100.0) * durationSec
    assertEquals(7200.0, calculatedSec, 0.001)

    val midTarget = 50.0
    val midSec = (midTarget / 100.0) * durationSec
    assertEquals(3600.0, midSec, 0.001)
  }

  @Test
  fun testEventObserverRegistration() {
    var receivedEvent = -1
    val observer = object : PlayerBridge.EventObserver {
      override fun event(eventId: Int) {
        receivedEvent = eventId
      }
    }

    PlayerBridge.addObserver(observer)
    PlayerBridge.notifyEvent(PlayerBridge.PlayerEvent.MPV_EVENT_FILE_LOADED)
    assertEquals(PlayerBridge.PlayerEvent.MPV_EVENT_FILE_LOADED, receivedEvent)
    PlayerBridge.removeObserver(observer)
  }

  @Test
  fun testVideoFlipFilterCommands() {
    // Add horizontal flip
    PlayerBridge.command("vf", "add", "@mpvex_hflip:hflip")
    assertTrue(ExoPlayerHolder.isHorizontalFlipped)

    // Remove horizontal flip
    PlayerBridge.command("vf", "remove", "@mpvex_hflip")
    assertFalse(ExoPlayerHolder.isHorizontalFlipped)

    // Add vertical flip
    PlayerBridge.command("vf", "add", "@mpvex_vflip:vflip")
    assertTrue(ExoPlayerHolder.isVerticalFlipped)

    // Remove vertical flip
    PlayerBridge.command("vf", "remove", "@mpvex_vflip")
    assertFalse(ExoPlayerHolder.isVerticalFlipped)
  }

  @Test
  fun testVolumeMultiplyCommand() {
    PlayerBridge.propInt["volume"].value = 100
    PlayerBridge.command("multiply", "volume", "0.5")
    assertEquals(50, PlayerBridge.propInt["volume"].value)

    PlayerBridge.command("multiply", "volume", "2")
    assertEquals(100, PlayerBridge.propInt["volume"].value)
  }
}

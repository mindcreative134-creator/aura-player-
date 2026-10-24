package app.marlboroadvance.mpvex.ui.player

import com.cmstudio.apnaplayer.engine.PlayerBridge
import com.cmstudio.apnaplayer.engine.PlayerNode

class PlayerObserver(
  private val activity: PlayerActivity,
) : PlayerBridge.EventObserver {
  override fun eventProperty(property: String) {
    if (activity.player.isExiting) return
    activity.runOnUiThread {
      if (activity.isFinishing || activity.isDestroyed || activity.player.isExiting) return@runOnUiThread
      activity.onObserverEvent(property)
    }
  }

  override fun eventProperty(
    property: String,
    value: Long,
  ) {
    if (activity.player.isExiting) return
    activity.runOnUiThread {
      if (activity.isFinishing || activity.isDestroyed || activity.player.isExiting) return@runOnUiThread
      activity.onObserverEvent(property, value)
    }
  }

  override fun eventProperty(
    property: String,
    value: Boolean,
  ) {
    if (activity.player.isExiting) return
    activity.runOnUiThread {
      if (activity.isFinishing || activity.isDestroyed || activity.player.isExiting) return@runOnUiThread
      activity.onObserverEvent(property, value)
    }
  }

  override fun eventProperty(
    property: String,
    value: String,
  ) {
    if (activity.player.isExiting) return
    activity.runOnUiThread {
      if (activity.isFinishing || activity.isDestroyed || activity.player.isExiting) return@runOnUiThread
      activity.onObserverEvent(property, value)
    }
  }

  override fun eventProperty(
    property: String,
    value: Double,
  ) {
    if (activity.player.isExiting) return
    activity.runOnUiThread {
      if (activity.isFinishing || activity.isDestroyed || activity.player.isExiting) return@runOnUiThread
      activity.onObserverEvent(property, value)
    }
  }

  @Suppress("EmptyFunctionBlock")
  override fun eventProperty(
    property: String,
    value: PlayerNode,
  ) {
    if (activity.player.isExiting) return
    activity.runOnUiThread {
      if (activity.isFinishing || activity.isDestroyed || activity.player.isExiting) return@runOnUiThread
      activity.onObserverEvent(property, value)
    }
  }

  override fun event(eventId: Int, data: PlayerNode) {
    if (activity.player.isExiting) return
    activity.runOnUiThread {
      if (activity.isFinishing || activity.isDestroyed || activity.player.isExiting) return@runOnUiThread
      activity.event(eventId)
    }
  }
}

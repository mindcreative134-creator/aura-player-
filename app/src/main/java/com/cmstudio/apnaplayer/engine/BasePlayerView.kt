package com.cmstudio.apnaplayer.engine

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

import android.view.LayoutInflater
import android.view.TextureView
import app.marlboroadvance.mpvex.R

/**
 * Base view embedding Google Media3 PlayerView with guaranteed hardware-accelerated TextureView
 * to support full-frame pinch-to-zoom and pan transformations.
 */
open class BasePlayerView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

  var isExiting = false

  val playerView: PlayerView = try {
    LayoutInflater.from(context).inflate(R.layout.view_player_texture, this, false) as PlayerView
  } catch (e: Exception) {
    PlayerView(context).apply {
      useController = false
      resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
      layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }
  }

  init {
    clipChildren = false
    clipToPadding = false
    playerView.clipChildren = false
    playerView.clipToPadding = false

    addView(playerView)
    val exo = ExoPlayerHolder.getOrCreatePlayer(context)
    playerView.player = exo
    ExoPlayerHolder.currentPlayerView = playerView
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    val exo = ExoPlayerHolder.getOrCreatePlayer(context)
    if (playerView.player !== exo) {
      playerView.player = exo
    }
    ExoPlayerHolder.currentPlayerView = playerView
    ExoPlayerHolder.applyVideoTransform()
  }

  open fun initOptions() {}
  open fun observeProperties() {}
  open fun postInitOptions() {}

  fun initialize(configDir: String, cacheDir: String) {
    initOptions()
    observeProperties()
    postInitOptions()
  }

  fun playFile(path: String) {
    ExoPlayerHolder.playFile(context, path)
  }

  fun setVo(vo: String) {
    // Media3 automatically utilizes hardware MediaCodec surfaces
  }

  fun destroy() {
    isExiting = true
    ExoPlayerHolder.stop()
    ExoPlayerHolder.release()
  }

  fun onPause() {
    ExoPlayerHolder.pause()
  }

  fun onResume() {
    val exo = ExoPlayerHolder.getOrCreatePlayer(context)
    if (playerView.player !== exo) {
      playerView.player = exo
    }
    ExoPlayerHolder.currentPlayerView = playerView
    ExoPlayerHolder.applyVideoTransform()
  }
}

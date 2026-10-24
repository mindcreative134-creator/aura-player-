package com.cmstudio.apnaplayer.engine

import android.view.KeyEvent

val keyMapping: Map<Int, String> = mapOf(
  KeyEvent.KEYCODE_SPACE to "SPACE",
  KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to "PLAYPAUSE",
  KeyEvent.KEYCODE_MEDIA_PLAY to "PLAY",
  KeyEvent.KEYCODE_MEDIA_PAUSE to "PAUSE",
  KeyEvent.KEYCODE_MEDIA_NEXT to "NEXT",
  KeyEvent.KEYCODE_MEDIA_PREVIOUS to "PREV",
  KeyEvent.KEYCODE_MEDIA_FAST_FORWARD to "FAST_FORWARD",
  KeyEvent.KEYCODE_MEDIA_REWIND to "REWIND",
  KeyEvent.KEYCODE_DPAD_LEFT to "LEFT",
  KeyEvent.KEYCODE_DPAD_RIGHT to "RIGHT",
  KeyEvent.KEYCODE_DPAD_UP to "UP",
  KeyEvent.KEYCODE_DPAD_DOWN to "DOWN",
  KeyEvent.KEYCODE_DPAD_CENTER to "ENTER",
  KeyEvent.KEYCODE_ENTER to "ENTER",
  KeyEvent.KEYCODE_VOLUME_UP to "VOLUME_UP",
  KeyEvent.KEYCODE_VOLUME_DOWN to "VOLUME_DOWN",
  KeyEvent.KEYCODE_BACK to "BACK"
)

package com.cmstudio.apnaplayer.engine

/**
 * Pure Kotlin representation of an engine property node.
 * Used for typed track, chapter, and metadata list parsing without C/JNI bindings.
 */
class PlayerNode(
  val value: Any? = null,
  private val serializedJson: String = "[]"
) {
  fun toJson(): String = serializedJson

  override fun toString(): String = serializedJson
}

package app.marlboroadvance.mpvex.domain.music

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Pass-through AudioProcessor that extracts real-time amplitude (0..1).
 * Powers Waveform HUD without needing Visualizer API or RECORD_AUDIO permission.
 */
@UnstableApi
class LevelAudioProcessor : BaseAudioProcessor() {

    val level = MutableStateFlow(0f)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) inputAudioFormat
        else AudioProcessor.AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return

        var sum = 0.0
        var count = 0
        val start = inputBuffer.position()
        val end = inputBuffer.limit()
        var i = start
        while (i + 1 < end) {
            val sample = (inputBuffer.get(i + 1).toInt() shl 8) or (inputBuffer.get(i).toInt() and 0xFF)
            sum += sample.toDouble() * sample
            count++
            i += 8
        }
        if (count > 0) {
            val rms = (sqrt(sum / count) / 32768.0).toFloat()
            val scaled = (rms * 3.5f).coerceIn(0f, 1f)
            level.value = max(scaled, level.value * 0.8f) // fast attack, smooth decay
        }

        val out = replaceOutputBuffer(size)
        out.put(inputBuffer)
        out.flip()
    }

    override fun onReset() {
        level.value = 0f
    }
}

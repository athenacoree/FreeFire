package com.athenacoree.music

import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal data class MusicEqualizerState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val bands: List<Int> = emptyList(),
    val frequenciesHz: List<Int> = emptyList(),
    val minLevel: Int = -1500,
    val maxLevel: Int = 1500
)

internal object MusicEqualizerController {
    private var effect: Equalizer? = null
    private var sessionId: Int = 0
    private val _state = MutableStateFlow(MusicEqualizerState())
    val state: StateFlow<MusicEqualizerState> = _state

    @Synchronized
    fun attach(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == sessionId && effect != null) return
        release()
        runCatching {
            val created = Equalizer(0, audioSessionId)
            val range = created.bandLevelRange
            val count = created.numberOfBands.toInt()
            val bands = (0 until count).map { created.getBandLevel(it.toShort()).toInt() }
            val frequencies = (0 until count).map { created.getCenterFreq(it.toShort()).toInt() / 1000 }
            created.enabled = false
            effect = created
            sessionId = audioSessionId
            _state.value = MusicEqualizerState(
                available = true,
                enabled = false,
                bands = bands,
                frequenciesHz = frequencies,
                minLevel = range[0].toInt(),
                maxLevel = range[1].toInt()
            )
        }.onFailure {
            effect = null
            sessionId = 0
            _state.value = MusicEqualizerState()
        }
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        runCatching {
            effect?.enabled = enabled
            _state.value = _state.value.copy(enabled = effect?.enabled == true)
        }
    }

    @Synchronized
    fun setBand(index: Int, level: Int) {
        val current = _state.value
        val target = effect ?: return
        if (index !in current.bands.indices) return
        runCatching {
            target.setBandLevel(index.toShort(), level.coerceIn(current.minLevel, current.maxLevel).toShort())
            val updated = current.bands.toMutableList()
            updated[index] = target.getBandLevel(index.toShort()).toInt()
            _state.value = current.copy(bands = updated)
        }
    }

    @Synchronized
    fun applyPreset(preset: Short) {
        val target = effect ?: return
        runCatching {
            if (preset.toInt() in 0 until target.numberOfPresets.toInt()) {
                target.usePreset(preset)
                val count = target.numberOfBands.toInt()
                _state.value = _state.value.copy(
                    bands = (0 until count).map { target.getBandLevel(it.toShort()).toInt() }
                )
            }
        }
    }

    @Synchronized
    fun release() {
        runCatching { effect?.release() }
        effect = null
        sessionId = 0
        _state.value = MusicEqualizerState()
    }
}

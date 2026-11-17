package com.mgradio.app.media

import android.media.audiofx.Equalizer
import com.mgradio.app.domain.model.EqualizerBandUi
import com.mgradio.app.domain.model.EqualizerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioEqualizerController {

    private var equalizer: Equalizer? = null
    private var attachedSessionId: Int = 0

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == attachedSessionId) return
        release()
        runCatching {
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = false
            equalizer = eq
            attachedSessionId = audioSessionId
            publishState(eq)
        }.onFailure {
            _state.value = EqualizerState(isSupported = false)
        }
    }

    fun setEnabled(enabled: Boolean) {
        val eq = equalizer ?: return
        runCatching {
            eq.enabled = enabled
            publishState(eq)
        }
    }

    fun selectPreset(presetIndex: Int) {
        val eq = equalizer ?: return
        runCatching {
            if (presetIndex in 0 until eq.numberOfPresets) {
                eq.usePreset(presetIndex.toShort())
                eq.enabled = true
                publishState(eq, presetIndex)
            }
        }
    }

    fun setBandLevel(bandIndex: Short, levelMb: Short) {
        val eq = equalizer ?: return
        runCatching {
            val range = eq.bandLevelRange
            val clamped = levelMb.coerceIn(range[0], range[1])
            eq.setBandLevel(bandIndex, clamped)
            eq.enabled = true
            publishState(eq, currentPreset = -1)
        }
    }

    private fun publishState(eq: Equalizer, currentPreset: Int = _state.value.currentPresetIndex) {
        val range = eq.bandLevelRange
        val minMb = range[0]
        val maxMb = range[1]
        val bands = (0 until eq.numberOfBands).map { idx ->
            val band = idx.toShort()
            EqualizerBandUi(
                index = band,
                centerFreqHz = eq.getCenterFreq(band) / 1000,
                minLevelMb = minMb,
                maxLevelMb = maxMb,
                currentLevelMb = eq.getBandLevel(band)
            )
        }
        val presets = (0 until eq.numberOfPresets).map { idx ->
            eq.getPresetName(idx.toShort())
        }
        _state.value = EqualizerState(
            isSupported = true,
            isEnabled = eq.enabled,
            presets = presets,
            currentPresetIndex = currentPreset,
            bands = bands
        )
    }

    fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
        attachedSessionId = 0
    }
}

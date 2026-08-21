package com.mgradio.app.domain.usecase

import com.mgradio.app.domain.model.Station
import com.mgradio.app.media.RadioPlayerManager
import javax.inject.Inject

class PlayStationUseCase @Inject constructor(
    private val radioPlayerManager: RadioPlayerManager
) {
    operator fun invoke(station: Station) {
        radioPlayerManager.playStation(station)
    }
}

class TogglePlayPauseUseCase @Inject constructor(
    private val radioPlayerManager: RadioPlayerManager
) {
    operator fun invoke() {
        radioPlayerManager.togglePlayPause()
    }
}

class StopAudioUseCase @Inject constructor(
    private val radioPlayerManager: RadioPlayerManager
) {
    operator fun invoke() {
        radioPlayerManager.stop()
    }
}

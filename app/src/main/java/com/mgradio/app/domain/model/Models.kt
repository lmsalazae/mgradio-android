package com.mgradio.app.domain.model

enum class ThemeMode(val label: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    AMOLED_DARK("Oscuro AMOLED")
}

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    RECONNECTING,
    PLAYING,
    PAUSED,
    ERROR
}

enum class StationTab(val label: String) {
    ALL("Todas"),
    FAVORITES("Favoritas"),
    RECENTS("Recientes"),
    OFFLINE("Offline")
}

data class Station(
    val id: String,
    val nombre: String,
    val frecuencia: String,
    val banda: String,
    val grupo: String,
    val streamUrl: String,
    val streamType: String,
    val logoUrl: String,
    val categoria: String,
    val pais: String,
    val ciudad: String,
    val tags: List<String> = emptyList(),
    val activo: Boolean = true,
    val orden: Int = 0,
    val isFavorite: Boolean = false,
    val lastChecked: Long = 0L
)

data class EqualizerBandUi(
    val index: Short,
    val centerFreqHz: Int,
    val minLevelMb: Short,
    val maxLevelMb: Short,
    val currentLevelMb: Short
)

data class EqualizerState(
    val isSupported: Boolean = false,
    val isEnabled: Boolean = false,
    val presets: List<String> = emptyList(),
    val currentPresetIndex: Int = -1,
    val bands: List<EqualizerBandUi> = emptyList()
)

data class RecordingState(
    val isRecording: Boolean = false,
    val stationName: String = "",
    val elapsedSeconds: Long = 0L,
    val bytesWritten: Long = 0L,
    val lastSavedFileName: String? = null
)

data class PlayerUiState(
    val currentStation: Station? = null,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val nowPlayingTitle: String? = null,
    val reconnectAttempt: Int = 0,
    val maxReconnectAttempts: Int = 3,
    val errorMessage: String? = null,
    val failedStation: Station? = null,
    val sleepTimerRemainingSeconds: Long? = null,
    val volumeFraction: Float = 0.7f,
    val recordingState: RecordingState = RecordingState(),
    val equalizerState: EqualizerState = EqualizerState()
)

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.AMOLED_DARK,
    val useDynamicColor: Boolean = false,
    val selectedCountry: String = ALL_COUNTRIES,
    val selectedCity: String = ALL_CITIES,
    val lastStationId: String? = null,
    val recordingTreeUri: String? = null,
    val recordingFolderLabel: String? = null,
    val sleepTimerPresetsMinutes: List<Int> = DEFAULT_SLEEP_PRESETS
) {
    companion object {
        const val ALL_COUNTRIES = "Todos"
        const val ALL_CITIES = "Todas"
        val DEFAULT_SLEEP_PRESETS = listOf(15, 30, 45, 60)
    }
}

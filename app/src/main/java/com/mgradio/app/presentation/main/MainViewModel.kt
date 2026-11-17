package com.mgradio.app.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mgradio.app.domain.model.PlaybackStatus
import com.mgradio.app.domain.model.PlayerUiState
import com.mgradio.app.domain.model.Station
import com.mgradio.app.domain.model.StationTab
import com.mgradio.app.domain.model.ThemeMode
import com.mgradio.app.domain.model.UserPreferences
import com.mgradio.app.domain.repository.PreferencesRepository
import com.mgradio.app.domain.repository.StationRepository
import com.mgradio.app.domain.usecase.GetAllStationsUseCase
import com.mgradio.app.domain.usecase.GetFavoriteStationsUseCase
import com.mgradio.app.domain.usecase.GetRecentStationsUseCase
import com.mgradio.app.domain.usecase.MarkStationStatusUseCase
import com.mgradio.app.domain.usecase.RecordRecentPlayUseCase
import com.mgradio.app.domain.usecase.RefreshStationsUseCase
import com.mgradio.app.domain.usecase.ToggleFavoriteUseCase
import com.mgradio.app.domain.util.StationUtils
import com.mgradio.app.media.RadioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val allStations: List<Station> = emptyList(),
    val filteredStations: List<Station> = emptyList(),
    val availableCountries: List<String> = listOf(UserPreferences.ALL_COUNTRIES),
    val availableCities: List<String> = listOf(UserPreferences.ALL_CITIES),
    val availableCategories: List<String> = listOf(ALL_CATEGORIES),
    val selectedTab: StationTab = StationTab.ALL,
    val selectedCountry: String = UserPreferences.ALL_COUNTRIES,
    val selectedCity: String = UserPreferences.ALL_CITIES,
    val selectedCategory: String = ALL_CATEGORIES,
    val searchQuery: String = "",
    val allCount: Int = 0,
    val favoritesCount: Int = 0,
    val recentsCount: Int = 0,
    val offlineCount: Int = 0,
    val isRefreshing: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val playerState: PlayerUiState = PlayerUiState(),
    val snackbarMessage: String? = null
) {
    companion object {
        const val ALL_CATEGORIES = "Todas"
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getAllStationsUseCase: GetAllStationsUseCase,
    private val getFavoriteStationsUseCase: GetFavoriteStationsUseCase,
    private val getRecentStationsUseCase: GetRecentStationsUseCase,
    private val refreshStationsUseCase: RefreshStationsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val markStationStatusUseCase: MarkStationStatusUseCase,
    private val recordRecentPlayUseCase: RecordRecentPlayUseCase,
    private val stationRepository: StationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val playerManager: RadioPlayerManager
) : ViewModel() {

    private data class FilterInput(
        val tab: StationTab = StationTab.ALL,
        val category: String = MainUiState.ALL_CATEGORIES,
        val searchQuery: String = "",
        val isRefreshing: Boolean = false
    )

    private val filterInput = MutableStateFlow(FilterInput())
    private val snackbarFlow = MutableStateFlow<String?>(null)

    private val listsFlow = combine(
        getAllStationsUseCase(),
        getFavoriteStationsUseCase(),
        getRecentStationsUseCase()
    ) { all, favs, recents -> Triple(all, favs, recents) }

    val uiState: StateFlow<MainUiState> = combine(
        listsFlow,
        preferencesRepository.preferencesFlow,
        filterInput,
        playerManager.playerState,
        snackbarFlow
    ) { (all, favs, recents), prefs, filters, player, snackbar ->
        val countries = listOf(UserPreferences.ALL_COUNTRIES) +
            all.map { it.pais }.filter { it.isNotBlank() }.distinct().sorted()

        val activeCountry = if (prefs.selectedCountry in countries) {
            prefs.selectedCountry
        } else {
            UserPreferences.ALL_COUNTRIES
        }

        val citiesForCountry = all
            .filter { activeCountry == UserPreferences.ALL_COUNTRIES || it.pais.equals(activeCountry, true) }
            .map { it.ciudad }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val cities = listOf(UserPreferences.ALL_CITIES) + citiesForCountry
        val activeCity = if (prefs.selectedCity in cities) prefs.selectedCity else UserPreferences.ALL_CITIES

        val categories = listOf(MainUiState.ALL_CATEGORIES) +
            all.filter {
                (activeCountry == UserPreferences.ALL_COUNTRIES || it.pais.equals(activeCountry, true)) &&
                    (activeCity == UserPreferences.ALL_CITIES || it.ciudad.equals(activeCity, true))
            }.map { it.categoria }.filter { it.isNotBlank() }.distinct().sorted()
        val activeCategory = if (filters.category in categories) filters.category else MainUiState.ALL_CATEGORIES

        val activeList = all.filter { it.activo }
        val offlineList = all.filter { !it.activo }

        fun applyFilters(list: List<Station>): List<Station> = StationUtils.filterAndSortStations(
            stations = list,
            query = filters.searchQuery,
            country = activeCountry,
            city = activeCity,
            category = activeCategory,
            allCountriesLabel = UserPreferences.ALL_COUNTRIES,
            allCitiesLabel = UserPreferences.ALL_CITIES,
            allCategoriesLabel = MainUiState.ALL_CATEGORIES
        )

        val filteredAll = applyFilters(activeList)
        val filteredFavs = applyFilters(favs)
        val filteredRecents = applyFilters(recents)
        val filteredOffline = applyFilters(offlineList)

        val filtered = when (filters.tab) {
            StationTab.ALL -> filteredAll
            StationTab.FAVORITES -> filteredFavs
            StationTab.RECENTS -> filteredRecents
            StationTab.OFFLINE -> filteredOffline
        }

        MainUiState(
            allStations = all,
            filteredStations = filtered,
            availableCountries = countries,
            availableCities = cities,
            availableCategories = categories,
            selectedTab = filters.tab,
            selectedCountry = activeCountry,
            selectedCity = activeCity,
            selectedCategory = activeCategory,
            searchQuery = filters.searchQuery,
            allCount = filteredAll.size,
            favoritesCount = filteredFavs.size,
            recentsCount = filteredRecents.size,
            offlineCount = filteredOffline.size,
            isRefreshing = filters.isRefreshing,
            preferences = prefs,
            playerState = player,
            snackbarMessage = snackbar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MainUiState()
    )

    init {
        restoreLastStation()
        refreshCatalog(silentIfNonEmpty = true)
        observePlayerErrors()
        observeRecordingCompletion()
    }

    private fun restoreLastStation() {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            val lastId = prefs.lastStationId ?: return@launch
            stationRepository.getStationById(lastId)?.let { station ->
                playerManager.restoreLastStationPreview(station)
            }
        }
    }

    private fun observePlayerErrors() {
        viewModelScope.launch {
            playerManager.playerState.collect { pState ->
                if (pState.status == PlaybackStatus.ERROR && pState.failedStation != null) {
                    markStationStatusUseCase(pState.failedStation.id, active = false)
                    pState.errorMessage?.let { showMessage(it) }
                    playerManager.clearError()
                }
            }
        }
    }

    private fun observeRecordingCompletion() {
        var prevSaved: String? = null
        viewModelScope.launch {
            playerManager.playerState.collect { pState ->
                val saved = pState.recordingState.lastSavedFileName
                if (saved != null && saved != prevSaved) {
                    prevSaved = saved
                    showMessage("Grabación guardada en Music/mgRadio/$saved")
                }
            }
        }
    }

    fun refreshCatalog(silentIfNonEmpty: Boolean = false) {
        viewModelScope.launch {
            if (!silentIfNonEmpty) {
                filterInput.update { it.copy(isRefreshing = true) }
            }
            refreshStationsUseCase()
                .onSuccess { count ->
                    if (!silentIfNonEmpty) {
                        showMessage("Catálogo actualizado ($count emisoras).")
                    }
                }
                .onFailure { err ->
                    if (!silentIfNonEmpty) {
                        showMessage("No se pudo sincronizar: ${err.message ?: "sin conexión"}")
                    }
                }
            filterInput.update { it.copy(isRefreshing = false) }
        }
    }

    fun selectTab(tab: StationTab) {
        filterInput.update { it.copy(tab = tab) }
    }

    fun selectCountry(country: String) {
        viewModelScope.launch {
            preferencesRepository.updatePreferences {
                it.copy(selectedCountry = country, selectedCity = UserPreferences.ALL_CITIES)
            }
        }
    }

    fun selectCity(city: String) {
        viewModelScope.launch {
            preferencesRepository.updatePreferences { it.copy(selectedCity = city) }
        }
    }

    fun selectCategory(category: String) {
        filterInput.update { it.copy(category = category) }
    }

    fun updateSearchQuery(query: String) {
        filterInput.update { it.copy(searchQuery = query) }
    }

    fun playStation(station: Station) {
        playerManager.playStation(station)
        viewModelScope.launch {
            if (!station.activo) {
                markStationStatusUseCase(station.id, active = true)
            }
            recordRecentPlayUseCase(station.id)
            preferencesRepository.updatePreferences { it.copy(lastStationId = station.id) }
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun stopPlayback() {
        playerManager.stop()
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch {
            toggleFavoriteUseCase(station.id, !station.isFavorite)
        }
    }

    fun setVolumeFraction(fraction: Float) {
        playerManager.setVolumeFraction(fraction)
    }

    fun syncVolume() {
        playerManager.syncSystemVolume()
    }

    fun startSleepTimer(minutes: Int) {
        playerManager.startSleepTimer(minutes) {
            showMessage("Temporizador finalizado: reproducción detenida.")
        }
        if (minutes > 0) {
            showMessage("Temporizador configurado a $minutes min.")
        }
    }

    fun cancelSleepTimer() {
        playerManager.cancelSleepTimer()
        showMessage("Temporizador cancelado.")
    }

    fun toggleRecording() {
        val treeUri = uiState.value.preferences.recordingTreeUri
        playerManager.toggleRecording(treeUri) { err -> showMessage(err) }
    }

    fun setRecordingFolder(treeUri: String, folderLabel: String) {
        viewModelScope.launch {
            preferencesRepository.updatePreferences {
                it.copy(recordingTreeUri = treeUri, recordingFolderLabel = folderLabel)
            }
            showMessage("Carpeta de grabaciones actualizada: $folderLabel")
        }
    }

    fun clearRecordingFolder() {
        viewModelScope.launch {
            preferencesRepository.updatePreferences {
                it.copy(recordingTreeUri = null, recordingFolderLabel = null)
            }
            showMessage("Carpeta restablecida a Music/mgRadio")
        }
    }

    fun toggleSleepTimerPreset(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePreferences { prefs ->
                val current = prefs.sleepTimerPresetsMinutes
                val updated = if (minutes in current) {
                    if (current.size > 1) current - minutes else current
                } else {
                    (current + minutes).sorted()
                }
                prefs.copy(sleepTimerPresetsMinutes = updated)
            }
        }
    }

    fun addCustomSleepTimerPreset(minutes: Int) {
        if (minutes !in 1..480) {
            showMessage("Ingresa un tiempo entre 1 y 480 minutos.")
            return
        }
        viewModelScope.launch {
            preferencesRepository.updatePreferences { prefs ->
                val updated = (prefs.sleepTimerPresetsMinutes + minutes).distinct().sorted()
                prefs.copy(sleepTimerPresetsMinutes = updated)
            }
            showMessage("Tiempo de $minutes min agregado al temporizador.")
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) = playerManager.setEqualizerEnabled(enabled)
    fun selectEqualizerPreset(index: Int) = playerManager.selectEqualizerPreset(index)
    fun setEqualizerBand(band: Short, levelMb: Short) = playerManager.setEqualizerBand(band, levelMb)

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.updatePreferences { it.copy(themeMode = mode) }
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updatePreferences { it.copy(useDynamicColor = enabled) }
        }
    }

    fun clearSnackbar() {
        snackbarFlow.value = null
    }

    private fun showMessage(msg: String) {
        snackbarFlow.value = msg
    }
}

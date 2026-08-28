package com.mgradio.app.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mgradio.app.domain.model.Station
import com.mgradio.app.domain.usecase.GetAllStationsUseCase
import com.mgradio.app.domain.usecase.GetFavoriteStationsUseCase
import com.mgradio.app.domain.usecase.MarkStationOfflineUseCase
import com.mgradio.app.domain.usecase.PlayStationUseCase
import com.mgradio.app.domain.usecase.RefreshStationsUseCase
import com.mgradio.app.domain.usecase.ToggleFavoriteUseCase
import com.mgradio.app.domain.usecase.TogglePlayPauseUseCase
import com.mgradio.app.media.PlaybackState
import com.mgradio.app.media.RadioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.mgradio.app.domain.util.StationUtils
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getAllStationsUseCase: GetAllStationsUseCase,
    private val getFavoriteStationsUseCase: GetFavoriteStationsUseCase,
    private val refreshStationsUseCase: RefreshStationsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val markStationOfflineUseCase: MarkStationOfflineUseCase,
    private val playStationUseCase: PlayStationUseCase,
    private val togglePlayPauseUseCase: TogglePlayPauseUseCase,
    private val radioPlayerManager: RadioPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadStations()
        observePlayerState()
        refreshRemoteData()
    }

    private fun loadStations() {
        combine(
            getAllStationsUseCase(),
            getFavoriteStationsUseCase()
        ) { stations, favorites ->
            val countries = StationUtils.extractCountries(stations)
            val categories = StationUtils.extractCategories(stations)

            val active = stations.filter { it.activo }
            val favActive = favorites.filter { it.activo }
            val offline = stations.filter { !it.activo }

            _uiState.update { currentState ->
                val cities = StationUtils.extractCities(stations, currentState.selectedCountry)
                val updatedPlayingStation = currentState.currentPlayingStation?.let { current ->
                    stations.find { it.id == current.id } ?: current
                }
                currentState.copy(
                    allStations = stations,
                    activeStations = active,
                    favoriteActiveStations = favActive,
                    offlineStations = offline,
                    categories = categories,
                    availableCountries = countries,
                    availableCities = cities,
                    currentPlayingStation = updatedPlayingStation
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun getCitiesForCountry(stations: List<Station>, country: String): List<String> {
        return StationUtils.extractCities(stations, country)
    }

    private fun observePlayerState() {
        radioPlayerManager.uiState.onEach { playerState ->
            if (playerState.playbackState == PlaybackState.ERROR && playerState.failedStation != null) {
                val failed = playerState.failedStation
                val explanation = playerState.errorMessage ?: "La emisora '${failed.nombre}' no se encuentra transmitiendo en este momento."

                viewModelScope.launch {
                    markStationOfflineUseCase(failed.id)
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        currentPlayingStation = null,
                        playbackState = PlaybackState.IDLE,
                        userMessage = "$explanation Se ha movido a la pestaña Offline."
                    )
                }
                radioPlayerManager.clearError()
            } else if (playerState.playbackState != PlaybackState.ERROR) {
                _uiState.update { currentState ->
                    val resolvedStation = playerState.currentStation?.let { playerStation ->
                        currentState.allStations.find { it.id == playerStation.id } ?: playerStation
                    }
                    currentState.copy(
                        currentPlayingStation = resolvedStation,
                        playbackState = playerState.playbackState
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun refreshRemoteData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                refreshStationsUseCase()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onCountrySelected(country: String) {
        _uiState.update { currentState ->
            val cities = getCitiesForCountry(currentState.allStations, country)
            currentState.copy(
                selectedCountry = country,
                selectedCity = "Todas las ciudades",
                availableCities = cities
            )
        }
    }

    fun onCitySelected(city: String) {
        _uiState.update { it.copy(selectedCity = city) }
    }

    fun onCategorySelected(category: String?) {
        _uiState.update { currentState ->
            val newCategory = if (currentState.selectedCategory == category) null else category
            currentState.copy(selectedCategory = newCategory)
        }
    }

    fun onTabSelected(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun onStationClicked(station: Station) {
        val latestStation = _uiState.value.allStations.find { it.id == station.id } ?: station
        playStationUseCase(latestStation)
    }

    fun onTogglePlayPause() {
        togglePlayPauseUseCase()
    }

    fun onToggleFavorite(station: Station) {
        viewModelScope.launch {
            val latestStation = _uiState.value.allStations.find { it.id == station.id } ?: station
            toggleFavoriteUseCase(station.id, !latestStation.isFavorite)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onClearSearchQuery() {
        _uiState.update { it.copy(searchQuery = "") }
    }

    fun onUserMessageDismissed() {
        _uiState.update { it.copy(userMessage = null) }
    }
}

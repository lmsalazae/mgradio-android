package com.mgradio.app.presentation.main

import com.mgradio.app.domain.model.Station
import com.mgradio.app.media.PlaybackState

data class MainUiState(
    val allStations: List<Station> = emptyList(),
    val activeStations: List<Station> = emptyList(),
    val favoriteActiveStations: List<Station> = emptyList(),
    val offlineStations: List<Station> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val selectedStationId: String? = null,
    val selectedCountry: String = "Todos los países",
    val selectedCity: String = "Todas las ciudades",
    val availableCountries: List<String> = listOf("Todos los países"),
    val availableCities: List<String> = listOf("Todas las ciudades"),
    val currentPlayingStation: Station? = null,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val isLoading: Boolean = false,
    val selectedTab: Int = 0, // 0: Todas (Activas), 1: Favoritas (Activas), 2: Offline
    val searchQuery: String = "",
    val userMessage: String? = null
)

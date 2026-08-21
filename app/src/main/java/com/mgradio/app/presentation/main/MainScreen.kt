package com.mgradio.app.presentation.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SignalWifiBad
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mgradio.app.domain.util.StationUtils
import com.mgradio.app.presentation.components.BottomPlayerBar
import com.mgradio.app.presentation.components.CitySelectorDropdown
import com.mgradio.app.presentation.components.CountrySelectorDropdown
import com.mgradio.app.presentation.components.FullScreenPlayerSheet
import com.mgradio.app.presentation.components.StationItemCard
import com.mgradio.app.presentation.components.StationSearchBar
import com.mgradio.app.presentation.theme.DarkBackground
import com.mgradio.app.presentation.theme.DarkSurface
import com.mgradio.app.presentation.theme.DarkSurfaceCard
import com.mgradio.app.presentation.theme.PrimaryCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var isPlayerSheetVisible by remember { mutableStateOf(false) }

    // Solicitud en tiempo de ejecución de permiso de notificaciones para Android 13+ (API 33+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ -> }

        LaunchedEffect(Unit) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onUserMessageDismissed()
        }
    }

    // Emisoras filtradas para cada pestaña con contadores dinámicos
    val filteredActiveStations = remember(uiState) {
        StationUtils.filterStations(
            stations = uiState.activeStations,
            country = uiState.selectedCountry,
            city = uiState.selectedCity,
            category = uiState.selectedCategory,
            searchQuery = uiState.searchQuery
        )
    }

    val filteredFavoriteStations = remember(uiState) {
        StationUtils.filterStations(
            stations = uiState.favoriteActiveStations,
            country = uiState.selectedCountry,
            city = uiState.selectedCity,
            category = uiState.selectedCategory,
            searchQuery = uiState.searchQuery
        )
    }

    val filteredOfflineStations = remember(uiState) {
        StationUtils.filterStations(
            stations = uiState.offlineStations,
            country = uiState.selectedCountry,
            city = uiState.selectedCity,
            category = uiState.selectedCategory,
            searchQuery = uiState.searchQuery
        )
    }

    val filteredStations = when (uiState.selectedTab) {
        1 -> filteredFavoriteStations
        2 -> filteredOfflineStations
        else -> filteredActiveStations
    }

    // Auto-scroll a la coincidencia más próxima al buscar o cambiar de filtro
    LaunchedEffect(
        uiState.searchQuery,
        uiState.selectedTab,
        uiState.selectedCountry,
        uiState.selectedCity,
        uiState.selectedCategory
    ) {
        if (filteredStations.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = DarkSurfaceCard,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        topBar = {
            Column(modifier = Modifier.background(DarkSurface)) {
                TopAppBar(
                    title = {
                        Text(
                            text = "mgRadio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.refreshRemoteData() },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Actualizar Catálogo",
                                tint = PrimaryCyan
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkSurface
                    )
                )

                // Cuadro de Búsqueda por Nombre
                StationSearchBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onClearQuery = { viewModel.onClearSearchQuery() }
                )

                // Selector de País
                CountrySelectorDropdown(
                    selectedCountry = uiState.selectedCountry,
                    availableCountries = uiState.availableCountries,
                    onCountrySelected = { country ->
                        viewModel.onCountrySelected(country)
                    }
                )

                // Selector de Ciudad
                CitySelectorDropdown(
                    selectedCity = uiState.selectedCity,
                    availableCities = uiState.availableCities,
                    onCitySelected = { city ->
                        viewModel.onCitySelected(city)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Tabs con Contadores Dinámicos: 0: Todas, 1: Favoritas, 2: Offline
                TabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor = DarkSurface,
                    contentColor = PrimaryCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                            color = PrimaryCyan,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.onTabSelected(0) },
                        text = {
                            Text(
                                "Todas (${filteredActiveStations.size})",
                                fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    Tab(
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.onTabSelected(1) },
                        text = {
                            Text(
                                "Favoritas (${filteredFavoriteStations.size})",
                                fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    Tab(
                        selected = uiState.selectedTab == 2,
                        onClick = { viewModel.onTabSelected(2) },
                        text = {
                            Text(
                                "Offline (${filteredOfflineStations.size})",
                                fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }

                // Filtro Horizontal de Categorías
                if (uiState.categories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        uiState.categories.forEach { category ->
                            val isSelected = uiState.selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onCategorySelected(category) },
                                label = { Text(category) },
                                modifier = Modifier.padding(end = 8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = PrimaryCyan
                                )
                            )
                        }
                    }
                }

                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = PrimaryCyan
                    )
                }
            }
        },
        bottomBar = {
            BottomPlayerBar(
                station = uiState.currentPlayingStation,
                playbackState = uiState.playbackState,
                onPlayPauseClick = { viewModel.onTogglePlayPause() },
                onBarClick = { isPlayerSheetVisible = true }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (filteredStations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                uiState.searchQuery.isNotBlank() -> Icons.Default.SearchOff
                                uiState.selectedTab == 1 -> Icons.Default.Favorite
                                uiState.selectedTab == 2 -> Icons.Default.SignalWifiBad
                                else -> Icons.Default.Radio
                            },
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = when {
                                uiState.searchQuery.isNotBlank() -> "No se encontraron emisoras que coincidan con \"${uiState.searchQuery}\"."
                                uiState.selectedTab == 1 -> "No tienes emisoras favoritas activas para los filtros seleccionados."
                                uiState.selectedTab == 2 -> "No hay emisoras fuera de línea para los filtros seleccionados."
                                else -> "No se encontraron emisoras activas para los filtros seleccionados."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = filteredStations,
                        key = { it.id }
                    ) { station ->
                        val isPlaying = uiState.currentPlayingStation?.id == station.id
                        StationItemCard(
                            station = station,
                            isPlaying = isPlaying,
                            playbackState = uiState.playbackState,
                            onStationClick = { viewModel.onStationClicked(it) },
                            onFavoriteClick = { viewModel.onToggleFavorite(it) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Modal reproductor expandido
    if (isPlayerSheetVisible) {
        FullScreenPlayerSheet(
            station = uiState.currentPlayingStation,
            playbackState = uiState.playbackState,
            onPlayPauseClick = { viewModel.onTogglePlayPause() },
            onFavoriteClick = { viewModel.onToggleFavorite(it) },
            onDismissRequest = { isPlayerSheetVisible = false }
        )
    }
}

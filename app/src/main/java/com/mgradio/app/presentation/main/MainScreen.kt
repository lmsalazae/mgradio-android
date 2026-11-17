package com.mgradio.app.presentation.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mgradio.app.domain.model.PlaybackStatus
import com.mgradio.app.domain.model.Station
import com.mgradio.app.domain.model.StationTab
import com.mgradio.app.presentation.player.BottomPlayerBar
import com.mgradio.app.presentation.player.FullScreenPlayerSheet
import com.mgradio.app.presentation.player.StationLogoBox
import com.mgradio.app.presentation.settings.SettingsSheet
import com.mgradio.app.presentation.theme.FavoritePink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var showFullPlayer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    Scaffold(
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                        viewModel.togglePlayPause()
                        true
                    }
                    Key.MediaStop -> {
                        viewModel.stopPlayback()
                        true
                    }
                    else -> false
                }
            } else false
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Text(
                        text = "mgRadio",
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                actions = {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.refreshCatalog(silentIfNonEmpty = false) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar emisoras")
                        }
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                }
            )
        },
        bottomBar = {
            if (state.playerState.currentStation != null) {
                BottomPlayerBar(
                    playerState = state.playerState,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onOpenFullPlayer = {
                        viewModel.syncVolume()
                        showFullPlayer = true
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = {
                    Text("Buscar por nombre, frecuencia o género...")
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                            focusManager.moveFocus(FocusDirection.Down)
                            true
                        } else false
                    }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterDropdownBox(
                    label = "País",
                    selected = state.selectedCountry,
                    options = state.availableCountries,
                    onSelect = viewModel::selectCountry,
                    modifier = Modifier.weight(1f)
                )
                FilterDropdownBox(
                    label = "Ciudad",
                    selected = state.selectedCity,
                    options = state.availableCities,
                    onSelect = viewModel::selectCity,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            ScrollableTabRow(
                selectedTabIndex = StationTab.entries.indexOf(state.selectedTab),
                edgePadding = 12.dp
            ) {
                StationTab.entries.forEach { tab ->
                    val count = when (tab) {
                        StationTab.ALL -> state.allCount
                        StationTab.FAVORITES -> state.favoritesCount
                        StationTab.RECENTS -> state.recentsCount
                        StationTab.OFFLINE -> state.offlineCount
                    }
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text("${tab.label} ($count)") }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.availableCategories.forEach { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category) }
                    )
                }
            }

            if (state.selectedTab == StationTab.OFFLINE) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Emisoras detectadas como offline o sin respuesta. Usa el botón de recarga (↻) para actualizar y validar si alguna volvió a estar en línea o pasó a offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (state.filteredStations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.allStations.isEmpty()) {
                            "Sincronizando catálogo de emisoras..."
                        } else {
                            "No se encontraron emisoras con estos filtros."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredStations, key = { it.id }) { station ->
                        val isPlaying = state.playerState.currentStation?.id == station.id &&
                            state.playerState.status == PlaybackStatus.PLAYING
                        StationItemCard(
                            station = station,
                            isCurrentPlaying = isPlaying,
                            onPlay = { viewModel.playStation(station) },
                            onToggleFavorite = { viewModel.toggleFavorite(station) }
                        )
                    }
                }
            }
        }
    }

    if (showFullPlayer && state.playerState.currentStation != null) {
        val current = state.allStations.firstOrNull { it.id == state.playerState.currentStation!!.id }
            ?: state.playerState.currentStation!!
        FullScreenPlayerSheet(
            station = current,
            playerState = state.playerState,
            sleepTimerPresets = state.preferences.sleepTimerPresetsMinutes,
            onDismiss = { showFullPlayer = false },
            onTogglePlayPause = viewModel::togglePlayPause,
            onStop = {
                viewModel.stopPlayback()
                showFullPlayer = false
            },
            onToggleFavorite = { viewModel.toggleFavorite(current) },
            onVolumeChange = viewModel::setVolumeFraction,
            onStartSleepTimer = viewModel::startSleepTimer,
            onCancelSleepTimer = viewModel::cancelSleepTimer,
            onToggleRecording = viewModel::toggleRecording,
            onSetEqEnabled = viewModel::setEqualizerEnabled,
            onSelectEqPreset = viewModel::selectEqualizerPreset,
            onSetEqBand = viewModel::setEqualizerBand,
            onCopiedUrl = { }
        )
    }

    if (showSettings) {
        SettingsSheet(
            preferences = state.preferences,
            onDismiss = { showSettings = false },
            onSelectTheme = viewModel::updateThemeMode,
            onToggleDynamicColor = viewModel::updateDynamicColor,
            onSelectRecordingFolder = viewModel::setRecordingFolder,
            onResetRecordingFolder = viewModel::clearRecordingFolder,
            onToggleSleepPreset = viewModel::toggleSleepTimerPreset,
            onAddCustomSleepPreset = viewModel::addCustomSleepTimerPreset
        )
    }
}

@Composable
private fun FilterDropdownBox(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .then(
                    if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                    else Modifier
                )
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selected,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        expanded = false
                        onSelect(opt)
                    }
                )
            }
        }
    }
}

@Composable
private fun StationItemCard(
    station: Station,
    isCurrentPlaying: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .then(
                if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StationLogoBox(logoUrl = station.logoUrl, sizeDp = 52)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = station.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrentPlaying) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Reproduciendo",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                val meta = listOf(
                    "${station.frecuencia} ${station.banda}".trim(),
                    station.ciudad,
                    station.categoria
                ).filter { it.isNotBlank() }.joinToString(" · ")
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (station.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (station.isFavorite) FavoritePink else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Reproducir ${station.nombre}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

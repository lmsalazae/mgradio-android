package com.mgradio.app.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mgradio.app.domain.model.EqualizerState
import com.mgradio.app.domain.model.PlaybackStatus
import com.mgradio.app.domain.model.PlayerUiState
import com.mgradio.app.domain.model.Station
import com.mgradio.app.presentation.theme.FavoritePink
import com.mgradio.app.presentation.theme.RecordRed
import com.mgradio.app.presentation.theme.tvFocusBorder

@Composable
fun BottomPlayerBar(
    playerState: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onOpenFullPlayer: () -> Unit
) {
    val station = playerState.currentStation ?: return
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary) else Modifier
            )
            .clickable(onClick = onOpenFullPlayer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StationLogoBox(logoUrl = station.logoUrl, sizeDp = 44)
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
                    if (playerState.recordingState.isRecording) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RecordRed)
                        )
                    }
                }
                val subtitle = when (playerState.status) {
                    PlaybackStatus.BUFFERING -> "Conectando..."
                    PlaybackStatus.RECONNECTING ->
                        "Reconectando (${playerState.reconnectAttempt}/${playerState.maxReconnectAttempts})..."
                    PlaybackStatus.PLAYING ->
                        playerState.nowPlayingTitle?.let { "♪ $it" }
                            ?: "${station.frecuencia} ${station.banda} · En vivo".trim()
                    PlaybackStatus.PAUSED -> "Pausado"
                    PlaybackStatus.ERROR -> "Error de conexión"
                    PlaybackStatus.IDLE -> "${station.frecuencia} ${station.banda}".trim()
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (playerState.status == PlaybackStatus.PLAYING) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (playerState.status == PlaybackStatus.BUFFERING ||
                playerState.status == PlaybackStatus.RECONNECTING
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.status == PlaybackStatus.PLAYING) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = "Reproducir o pausar"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayerSheet(
    station: Station,
    playerState: PlayerUiState,
    sleepTimerPresets: List<Int>,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onToggleFavorite: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onToggleRecording: () -> Unit,
    onSetEqEnabled: (Boolean) -> Unit,
    onSelectEqPreset: (Int) -> Unit,
    onSetEqBand: (Short, Short) -> Unit,
    onCopiedUrl: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    var showSleepDialog by remember { mutableStateOf(false) }
    var showEqSheet by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StationLogoBox(logoUrl = station.logoUrl, sizeDp = 128)

            // Badge de estado / grabación
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusLabel = when (playerState.status) {
                    PlaybackStatus.PLAYING -> "EN VIVO"
                    PlaybackStatus.BUFFERING -> "CONECTANDO..."
                    PlaybackStatus.RECONNECTING ->
                        "RECONECTANDO (${playerState.reconnectAttempt}/${playerState.maxReconnectAttempts})"
                    PlaybackStatus.PAUSED -> "PAUSADO"
                    PlaybackStatus.ERROR -> "ERROR"
                    PlaybackStatus.IDLE -> "LISTO"
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (playerState.status == PlaybackStatus.PLAYING) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (playerState.status == PlaybackStatus.PLAYING) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                if (playerState.recordingState.isRecording) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = RecordRed.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "REC ${formatSeconds(playerState.recordingState.elapsedSeconds)}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = RecordRed
                        )
                    }
                }

                playerState.sleepTimerRemainingSeconds?.let { rem ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "⏱ ${formatSeconds(rem)}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                text = station.nombre,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = listOf("${station.frecuencia} ${station.banda}".trim(), station.ciudad, station.pais)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Banner Now Playing (ICY)
            playerState.nowPlayingTitle?.let { icy ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "♪ $icy",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Controles principales
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = if (station.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (station.isFavorite) FavoritePink else MaterialTheme.colorScheme.onSurface
                    )
                }

                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(68.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.status == PlaybackStatus.PLAYING) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = "Reproducir o pausar",
                        modifier = Modifier.size(34.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Detener")
                }
            }

            // Herramientas: Grabar, Ecualizador, Sleep Timer, Copiar URL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ToolActionButton(
                    icon = {
                        Icon(
                            Icons.Default.FiberManualRecord,
                            contentDescription = "Grabar",
                            tint = if (playerState.recordingState.isRecording) RecordRed else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    label = if (playerState.recordingState.isRecording) "Detener" else "Grabar",
                    onClick = onToggleRecording
                )
                ToolActionButton(
                    icon = { Icon(Icons.Default.Equalizer, contentDescription = "Ecualizador") },
                    label = "Ecualizador",
                    onClick = { showEqSheet = true }
                )
                ToolActionButton(
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Temporizador") },
                    label = "Sleep",
                    onClick = { showSleepDialog = true }
                )
                ToolActionButton(
                    icon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copiar URL") },
                    label = "Copiar URL",
                    onClick = {
                        clipboard.setText(AnnotatedString(station.streamUrl))
                        onCopiedUrl()
                    }
                )
            }

            // Control de volumen
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onVolumeChange(0f) }) {
                    Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "Silenciar")
                }
                Slider(
                    value = playerState.volumeFraction,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onVolumeChange(1f) }) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Volumen máximo")
                }
            }

            // Detalles de la emisora
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DetailRow("Grupo", station.grupo.ifBlank { "Independiente" })
                    DetailRow("Categoría", station.categoria)
                    if (station.tags.isNotEmpty()) {
                        DetailRow("Etiquetas", station.tags.joinToString(", "))
                    }
                    DetailRow("Stream", station.streamType.ifBlank { "HTTP Audio" })
                }
            }
        }
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            presetsMinutes = sleepTimerPresets,
            isActive = playerState.sleepTimerRemainingSeconds != null,
            onDismiss = { showSleepDialog = false },
            onSelectMinutes = { mins ->
                showSleepDialog = false
                onStartSleepTimer(mins)
            },
            onCancelTimer = {
                showSleepDialog = false
                onCancelSleepTimer()
            }
        )
    }

    if (showEqSheet) {
        EqualizerSheet(
            state = playerState.equalizerState,
            onDismiss = { showEqSheet = false },
            onSetEnabled = onSetEqEnabled,
            onSelectPreset = onSelectEqPreset,
            onSetBand = onSetEqBand
        )
    }
}

@Composable
private fun ToolActionButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .tvFocusBorder(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SleepTimerDialog(
    presetsMinutes: List<Int>,
    isActive: Boolean,
    onDismiss: () -> Unit,
    onSelectMinutes: (Int) -> Unit,
    onCancelTimer: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Temporizador de apagado") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetsMinutes.forEach { mins ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusBorder(RoundedCornerShape(10.dp))
                            .clickable { onSelectMinutes(mins) }
                    ) {
                        Text(
                            text = "$mins minutos",
                            modifier = Modifier.padding(14.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isActive) {
                TextButton(onClick = onCancelTimer) {
                    Text("Cancelar timer", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    state: EqualizerState,
    onDismiss: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onSetBand: (Short, Short) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ecualizador",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (state.isSupported) {
                    Switch(checked = state.isEnabled, onCheckedChange = onSetEnabled)
                }
            }

            if (!state.isSupported) {
                Text(
                    text = "Inicia la reproducción de una emisora para activar el ecualizador.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (state.presets.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.presets.forEachIndexed { idx, name ->
                            FilterChip(
                                selected = idx == state.currentPresetIndex,
                                onClick = { onSelectPreset(idx) },
                                label = { Text(name) }
                            )
                        }
                    }
                }

                state.bands.forEach { band ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${band.centerFreqHz} Hz", style = MaterialTheme.typography.labelMedium)
                            Text("${band.currentLevelMb / 100} dB", style = MaterialTheme.typography.labelMedium)
                        }
                        Slider(
                            value = band.currentLevelMb.toFloat(),
                            onValueChange = { v -> onSetBand(band.index, v.toInt().toShort()) },
                            valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
                            enabled = state.isEnabled
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StationLogoBox(logoUrl: String, sizeDp: Int) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl.isNotBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Radio,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size((sizeDp * 0.5f).dp)
            )
        }
    }
}

private fun formatSeconds(totalSec: Long): String {
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "%02d:%02d".format(mins, secs)
}

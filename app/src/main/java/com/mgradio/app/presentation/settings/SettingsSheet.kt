package com.mgradio.app.presentation.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mgradio.app.domain.model.ThemeMode
import com.mgradio.app.domain.model.UserPreferences

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    preferences: UserPreferences,
    onDismiss: () -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onSelectRecordingFolder: (uri: String, label: String) -> Unit,
    onResetRecordingFolder: () -> Unit,
    onToggleSleepPreset: (Int) -> Unit,
    onAddCustomSleepPreset: (Int) -> Unit
) {
    val context = LocalContext.current
    var customMinutesText by remember { mutableStateOf("") }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            val label = uri.lastPathSegment
                ?.substringAfter(':')
                ?.ifBlank { uri.lastPathSegment }
                ?: "Carpeta personalizada"
            onSelectRecordingFolder(uri.toString(), label)
        }
    }

    val availableSleepOptions = remember(preferences.sleepTimerPresetsMinutes) {
        (listOf(5, 10, 15, 20, 30, 45, 60, 90, 120) + preferences.sleepTimerPresetsMinutes)
            .distinct()
            .sorted()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Ajustes de mgRadio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text("Tema visual", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = preferences.themeMode == mode,
                        onClick = { onSelectTheme(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleDynamicColor(!preferences.useDynamicColor) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Material You (Color dinámico)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = preferences.useDynamicColor,
                        onCheckedChange = onToggleDynamicColor
                    )
                }
            }

            HorizontalDivider()

            Text("Carpeta de grabaciones", style = MaterialTheme.typography.labelLarge)
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = preferences.recordingFolderLabel ?: "Music/mgRadio (Predeterminada)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { folderPickerLauncher.launch(null) }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Elegir carpeta")
                        }
                        if (preferences.recordingTreeUri != null) {
                            TextButton(
                                onClick = onResetRecordingFolder,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Restablecer")
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Text("Tiempos de apagado (Sleep Timer)", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "Selecciona los tiempos disponibles en el reproductor:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableSleepOptions.forEach { mins ->
                    val active = mins in preferences.sleepTimerPresetsMinutes
                    FilterChip(
                        selected = active,
                        onClick = { onToggleSleepPreset(mins) },
                        label = { Text("$mins min") }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customMinutesText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 3) {
                            customMinutesText = input
                        }
                    },
                    label = { Text("Agregar tiempo (min)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val mins = customMinutesText.toIntOrNull()
                        if (mins != null) {
                            onAddCustomSleepPreset(mins)
                            customMinutesText = ""
                        }
                    },
                    enabled = (customMinutesText.toIntOrNull() ?: 0) in 1..480
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar tiempo")
                }
            }

            HorizontalDivider()

            Text(
                text = "Privacidad: mgRadio no recopila analíticas ni telemetría.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

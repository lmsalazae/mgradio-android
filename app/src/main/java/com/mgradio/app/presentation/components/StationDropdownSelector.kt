package com.mgradio.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mgradio.app.presentation.theme.DarkSurfaceCard
import com.mgradio.app.presentation.theme.PrimaryCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectorDropdown(
    selectedCountry: String,
    availableCountries: List<String>,
    onCountrySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCountry,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            shape = RoundedCornerShape(12.dp),
            label = {
                Text(
                    text = "País",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceCard,
                unfocusedContainerColor = DarkSurfaceCard,
                focusedBorderColor = PrimaryCyan,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) {
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            expanded = !expanded
                        }
                        true
                    } else if (keyEvent.key == Key.DirectionRight) {
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            focusManager.moveFocus(FocusDirection.Right)
                        }
                        true
                    } else if (keyEvent.key == Key.DirectionLeft) {
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            focusManager.moveFocus(FocusDirection.Left)
                        }
                        true
                    } else {
                        false
                    }
                }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableCountries.forEach { country ->
                val isSelected = country == selectedCountry
                DropdownMenuItem(
                    text = {
                        Text(
                            text = country,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PrimaryCyan else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onCountrySelected(country)
                        expanded = false
                    },
                    modifier = Modifier.onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) {
                            if (keyEvent.type == KeyEventType.KeyUp) {
                                onCountrySelected(country)
                                expanded = false
                            }
                            true
                        } else {
                            false
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelectorDropdown(
    selectedCity: String,
    availableCities: List<String>,
    onCitySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCity,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            shape = RoundedCornerShape(12.dp),
            label = {
                Text(
                    text = "Ciudad",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceCard,
                unfocusedContainerColor = DarkSurfaceCard,
                focusedBorderColor = PrimaryCyan,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) {
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            expanded = !expanded
                        }
                        true
                    } else if (keyEvent.key == Key.DirectionRight) {
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            focusManager.moveFocus(FocusDirection.Right)
                        }
                        true
                    } else if (keyEvent.key == Key.DirectionLeft) {
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            focusManager.moveFocus(FocusDirection.Left)
                        }
                        true
                    } else {
                        false
                    }
                }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableCities.forEach { city ->
                val isSelected = city == selectedCity
                DropdownMenuItem(
                    text = {
                        Text(
                            text = city,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PrimaryCyan else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onCitySelected(city)
                        expanded = false
                    },
                    modifier = Modifier.onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) {
                            if (keyEvent.type == KeyEventType.KeyUp) {
                                onCitySelected(city)
                                expanded = false
                            }
                            true
                        } else {
                            false
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StationDropdownSelectors(
    selectedCountry: String,
    availableCountries: List<String>,
    onCountrySelected: (String) -> Unit,
    selectedCity: String,
    availableCities: List<String>,
    onCitySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CountrySelectorDropdown(
            selectedCountry = selectedCountry,
            availableCountries = availableCountries,
            onCountrySelected = onCountrySelected,
            modifier = Modifier.weight(1f)
        )

        CitySelectorDropdown(
            selectedCity = selectedCity,
            availableCities = availableCities,
            onCitySelected = onCitySelected,
            modifier = Modifier.weight(1f)
        )
    }
}

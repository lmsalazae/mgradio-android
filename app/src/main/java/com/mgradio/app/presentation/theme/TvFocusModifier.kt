package com.mgradio.app.presentation.theme

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Resalta con un borde de 2.dp el elemento enfocado mediante D-Pad / control remoto de TV.
 */
fun Modifier.tvFocusBorder(shape: Shape = RectangleShape): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val borderColor = MaterialTheme.colorScheme.primary
    this
        .onFocusChanged { focused = it.isFocused || it.hasFocus }
        .then(
            if (focused) Modifier.border(2.dp, borderColor, shape) else Modifier
        )
}

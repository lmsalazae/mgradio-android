package com.mgradio.app

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Suite End-to-End (E2E) para mgRadio sobre dispositivo/emulador real.
 *
 * Opcionalmente captura screenshots vía MediaStore cuando se pasa:
 * `-Pandroid.testInstrumentationRunnerArguments.captureScreenshots=true`
 */
@RunWith(AndroidJUnit4::class)
class MgRadioE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun shouldCaptureScreenshots(): Boolean =
        InstrumentationRegistry.getArguments().getString("captureScreenshots", "false").toBoolean()

    private fun snapIfEnabled(name: String) {
        if (!shouldCaptureScreenshots()) return
        composeRule.waitForIdle()
        Thread.sleep(450)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap: Bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val resolver = instrumentation.targetContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/mgRadioShots")
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    @Test
    fun mainScreenAndSettingsWorkflow() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("mgRadio").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Actualizar emisoras").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Ajustes").assertIsDisplayed()
        snapIfEnabled("01-radio-home.png")

        // Refrescar catálogo de emisoras y probar búsqueda si el campo está visible
        composeRule.onNodeWithContentDescription("Actualizar emisoras").performClick()
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithText("Buscar", substring = true).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onAllNodesWithText("Buscar", substring = true).onFirst().performTextInput("Radio")
            composeRule.waitForIdle()
            if (composeRule.onAllNodesWithContentDescription("Limpiar búsqueda").fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithContentDescription("Limpiar búsqueda").performClick()
                composeRule.waitForIdle()
            }
        }

        // Abrir hoja de Ajustes y validar controles de tema, Material You y privacidad
        composeRule.onNodeWithContentDescription("Ajustes").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ajustes de mgRadio").assertIsDisplayed()
        composeRule.onNodeWithText("Material You (Color dinámico)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Material You (Color dinámico)").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Material You (Color dinámico)").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Privacidad: mgRadio no recopila analíticas ni telemetría.").assertExists()
        snapIfEnabled("02-radio-settings.png")
    }
}

package com.lebaillyapp.reactivebox.ui.composition

import com.lebaillyapp.reactivebox.R
import android.graphics.RuntimeShader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lebaillyapp.reactivebox.ui.viewModel.SensorViewModel

/**
 * ReactiveBox - Mode Dynamique (Interactive)
 *
 * Affiche le shader AGSL avec uTilt mis à jour par le SensorViewModel (Rotation Vector).
 *
 * @param viewModel Le SensorViewModel fournissant les données d'inclinaison.
 */
@Composable
fun ReactiveBoxInteractive(
    viewModel: SensorViewModel
) {
    val context = LocalContext.current

    // 1. COLLECTE DES DONNÉES DYNAMIQUES DU CAPTEUR
    // Convertit le StateFlow<Offset> du ViewModel en état Compose.
    val tiltOffset by viewModel.tilt.collectAsState()

    // Simplification pour l'usage dans le Canvas et le Debug Overlay
    val tiltX = tiltOffset.x
    val tiltY = tiltOffset.y

    // 2. Chargement du shader depuis res/raw
    val shaderSource = remember {
        try {
            context.resources
                .openRawResource(R.raw.reactive_box_v10)
                .bufferedReader()
                .use { it.readText() }
        } catch (e: Exception) {
            throw IllegalStateException(
                "Impossible de charger reactive_box_v10.agsl. " +
                        "Vérifiez que le fichier existe dans res/raw/",
                e
            )
        }
    }

    // 3. Création du RuntimeShader
    val shader = remember(shaderSource) { RuntimeShader(shaderSource) }

    // 4. Animation du temps (pour les effets dynamiques dans le shader)
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { millis ->
                time = (millis / 1000f) % 3600f
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A10)),
        contentAlignment = Alignment.Center
    ) {
        // 5. Canvas avec ShaderBrush
        Canvas(
            modifier = Modifier
                .fillMaxWidth(1f)
                .aspectRatio(1f)
        ) {
            // Configuration des uniforms DANS LE DRAW SCOPE

            shader.setFloatUniform("uTilt", tiltX, tiltY)
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("uTime", time)
            shader.setFloatUniform("uEdgeThickness", 0.005f)
            shader.setFloatUniform("uRimIntensity", 0.50f)
            shader.setFloatUniform("uSpecularPower", 32f)
            shader.setFloatUniform("uNoiseStrength", 0.005f)

            // Dessin avec le shader
            drawRect(ShaderBrush(shader))
        }

        // 6. Debug overlay (Affichage des valeurs réelles)
        Text(
            text = """
                ReactiveBox - Interactive
                Tilt: X=${tiltX.formatDecimal(2)} Y=${tiltY.formatDecimal(2)}
                Time: ${time.formatDecimal(2)}s
                
            """.trimIndent(),
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
    }
}

/**
 * Extension pour formater les floats (Doit être dans le même fichier ou importable)
 */
private fun Float.formatDecimal(decimals: Int): String {
    return "%.${decimals}f".format(this)
}
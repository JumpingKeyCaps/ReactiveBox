package com.lebaillyapp.reactivebox.ui.composition

import com.lebaillyapp.reactivebox.R
import android.graphics.RuntimeShader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

/**
 * ReactiveBox - Mode Statique (Vanilla)
 *
 * Affiche le shader AGSL avec uTilt forcé à (0, 0)
 * pour valider le rendu sans capteurs.
 *
 * Pré-requis:
 * - Fichier shader dans res/raw/shader_debug.agsl
 * - minSdk 33 (Android 13+)
 * - Device réel (émulateur = performances limitées)
 */
@Composable
fun ReactiveBoxStatic() {
    val context = LocalContext.current

    // 1. Chargement du shader depuis res/raw
    val shaderSource = remember {
        try {
            context.resources
                .openRawResource(R.raw.shader_debug)
                .bufferedReader()
                .use { it.readText() }
        } catch (e: Exception) {
            throw IllegalStateException(
                "Impossible de charger shader_debug.agsl. " +
                        "Vérifiez que le fichier existe dans res/raw/",
                e
            )
        }
    }

    // 2. Création du RuntimeShader
    val shader = remember(shaderSource) { RuntimeShader(shaderSource) }

    // 3. Animation du temps
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
            .background(Color(0xFF0A0A10))
    ) {
        // 4. Canvas avec ShaderBrush (la bonne approche pour AGSL)
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Configuration des uniforms ICI, dans le drawScope
            shader.setFloatUniform("uTilt", 0f, 0f)
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("uTime", time)
            shader.setFloatUniform("uEdgeThickness", 0.002f)
            shader.setFloatUniform("uRimIntensity", 1.0f)
            shader.setFloatUniform("uSpecularPower", 32f)
            shader.setFloatUniform("uNoiseStrength", 0.005f)

            // Dessin avec le shader
            drawRect(ShaderBrush(shader))
        }

        // 5. Debug overlay
        Text(
            text = """
                🎨 ReactiveBox - Mode Statique
                Tilt: X=0.00 Y=0.00 (forcé)
                Time: ${time.formatDecimal(2)}s
                
                 Shader actif
                 Capteurs désactivés
            """.trimIndent(),
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
    }
}

/**
 * Extension pour formater les floats
 */
private fun Float.formatDecimal(decimals: Int): String {
    return "%.${decimals}f".format(this)
}
package com.lebaillyapp.reactivebox.ui.composition

import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lebaillyapp.reactivebox.ui.viewModel.SensorViewModel

// Ajoutez la fonction d'extension de formatage (si elle n'est pas déjà là)
private fun Float.format(decimals: Int) = "%.${decimals}f".format(this)


@Composable
fun SensorDebugView(viewModel: SensorViewModel) {
    // Collecte des valeurs d'inclinaison du ViewModel
    val tilt by viewModel.tilt.collectAsState()

    // Constante pour ajuster la sensibilité du déplacement visuel
    val VISUAL_SENSITIVITY = 100 // Déplace le cercle de 100px/dp par radian d'inclinaison

    // Calcul de l'offset visuel en dp (Float en dp)
    // tilt.x est le Roll (mouvement horizontal)
    // tilt.y est le Pitch (mouvement vertical)
    val offsetX = (tilt.x * VISUAL_SENSITIVITY).dp
    val offsetY = (tilt.y * VISUAL_SENSITIVITY).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101015)), // Fond sombre
        contentAlignment = Alignment.Center
    ) {
        // --- 1. Cercle de translation ---
        Box(
            modifier = Modifier
                .offset(x = offsetX, y = offsetY) // Applique la translation
                .size(40.dp)
                .background(Color.Red)
        ) {
            // Un simple indicateur pour le point central (au centre du cercle)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(size.width / 2, size.height / 2)
                )
            }
        }

        // --- 2. Visualisation des valeurs (Overlay) ---
        Text(
            text = "Tilt (Roll/X): ${tilt.x.format(3)} rad\n" +
                    "Tilt (Pitch/Y): ${tilt.y.format(3)} rad\n" +
                    "OffsetX: ${offsetX.value.format(1)} dp\n" +
                    "OffsetY: ${offsetY.value.format(1)} dp",
            color = Color.Cyan,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(32.dp)
        )

        // Indication de la plage de Clamping
        Text(
            text = "Clamping: [-1.5f, 1.5f] rad",
            color = Color.Gray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
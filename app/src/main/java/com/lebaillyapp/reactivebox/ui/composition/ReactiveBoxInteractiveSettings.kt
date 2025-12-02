package com.lebaillyapp.reactivebox.ui.composition

import com.lebaillyapp.reactivebox.R
import android.graphics.RuntimeShader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lebaillyapp.reactivebox.ui.viewModel.SensorViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.toArgb
// Importation nécessaire pour la manipulation HSV standard sur Android
import android.graphics.Color as AndroidColor
import kotlin.math.roundToInt

/**
 * Extension pour formater les floats (Doit être dans le même fichier ou importable)
 */
private fun Float.formatDecimal(decimals: Int): String {
    val decimalPlaces = if (decimals > 0) 3 else 2
    return "%.${decimalPlaces}f".format(this)
}

/**
 * Utility Object pour gérer les conversions de couleur HSV
 */
object HsvColorUtils {
    /** Convertit Compose Color en un tableau [H, S, V] (0..360, 0..1, 0..1) */
    fun toHsv(color: Color): FloatArray {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color.toArgb(), hsv)
        return hsv // [hue, saturation, value]
    }

    /** Crée une Compose Color à partir de H, S, V, A. */
    fun fromHsv(h: Float, s: Float, v: Float, a: Float = 1f): Color {
        val rgbInt = AndroidColor.HSVToColor(floatArrayOf(h, s, v))
        return Color(rgbInt).copy(alpha = a)
    }
}


/**
 * Data class pour contenir tous les réglages du shader, incluant les 9 couleurs des faces.
 */
data class ShaderSettings(
    // Visuels Généraux
    val edgeThickness: Float = 0.005f,
    val rimIntensity: Float = 0.50f,
    val faceBrightness: Float = 0.9f,

    // Ambient Occlusion
    val aoStrength: Float = 0.2f,
    val aoRadius: Float = 0.3f,

    // Couleurs Néon
    val neonColor: Color = Color(0xFF00FFFF), // Cyan
    val highlightColor: Color = Color(0xFFF2F8FF), // Blanc

    // Couleurs des Faces (répliquant les anciennes constantes)
    val colorBox: Color = Color(0xFF263359),          // #263359 (0.15, 0.2, 0.35)
    val colorTopLight: Color = Color(0xFFFFa54d),    // #FFa54d (1.0, 0.65, 0.3)
    val colorTopDark: Color = Color(0xFF995926),     // #995926 (0.6, 0.35, 0.15)
    val colorBottomLight: Color = Color(0xFF4dE680),  // #4dE680 (0.3, 0.9, 0.5)
    val colorBottomDark: Color = Color(0xFF1A6640),   // #1A6640 (0.1, 0.4, 0.25)
    val colorLeftLight: Color = Color(0xFF66F2FF),    // #66F2FF (0.4, 0.95, 1.0)
    val colorLeftDark: Color = Color(0xFF26738c),     // #26738c (0.15, 0.45, 0.55)
    val colorRightLight: Color = Color(0xFFFF80E6),   // #FF80E6 (1.0, 0.5, 0.9)
    val colorRightDark: Color = Color(0xFF803373)     // #803373 (0.5, 0.2, 0.45)
)

/**
 * Composition Interactive Principale avec panneau de réglages.
 *
 * @param viewModel Le SensorViewModel fournissant les données d'inclinaison.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactiveBoxInteractiveSettings(
    viewModel: SensorViewModel
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showBottomSheet by remember { mutableStateOf(false) }

    // État principal des réglages AGSL
    var settings by remember { mutableStateOf(ShaderSettings()) }

    // 1. COLLECTE DES DONNÉES DYNAMIQUES DU CAPTEUR
    val tiltOffset by viewModel.tilt.collectAsState()
    val tiltX = tiltOffset.x
    val tiltY = tiltOffset.y

    // 2. Chargement du shader (v11)
    val shaderSource = remember {
        try {
            // NOTE: R.raw.reactive_box_v11 doit exister dans le projet Android
            context.resources
                .openRawResource(R.raw.reactive_box_v11)
                .bufferedReader()
                .use { it.readText() }
        } catch (e: Exception) {
            throw IllegalStateException(
                "Impossible de charger reactive_box_v11.agsl. " +
                        "Vérifiez que le fichier existe dans res/raw/",
                e
            )
        }
    }
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0A0A10), // Fond foncé
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.tertiary,
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Réglages Shader")
            }
        }
    ) { paddingValues ->
        // Contenu Principal (Le Canvas et le Debug Overlay)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {

            // Le Canvas avec le Shader
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .aspectRatio(1f)
            ) {
                // MISE À JOUR DYNAMIQUE DES UNIFORMS (Capteur + Réglages)

                // Capteur
                shader.setFloatUniform("uTilt", tiltX, tiltY)

                // Réglages Visuels
                shader.setFloatUniform("uEdgeThickness", settings.edgeThickness)
                shader.setFloatUniform("uRimIntensity", settings.rimIntensity)
                shader.setFloatUniform("uFaceBrightness", settings.faceBrightness)
                shader.setFloatUniform("uAOStrength", settings.aoStrength)
                shader.setFloatUniform("uAORadius", settings.aoRadius)

                // Couleurs Néon (Passées comme float3 RGB)
                shader.setFloatUniform("uNeonLineColor", settings.neonColor.red, settings.neonColor.green, settings.neonColor.blue)
                shader.setFloatUniform("uHighlightColor", settings.highlightColor.red, settings.highlightColor.green, settings.highlightColor.blue)

                // Couleurs des Faces (Passées comme float3 RGB)
                fun setFaceColorUniform(uniformName: String, color: Color) {
                    shader.setFloatUniform(uniformName, color.red, color.green, color.blue)
                }

                setFaceColorUniform("uColorBox", settings.colorBox)
                setFaceColorUniform("uColorTopLight", settings.colorTopLight)
                setFaceColorUniform("uColorTopDark", settings.colorTopDark)
                setFaceColorUniform("uColorBottomLight", settings.colorBottomLight)
                setFaceColorUniform("uColorBottomDark", settings.colorBottomDark)
                setFaceColorUniform("uColorLeftLight", settings.colorLeftLight)
                setFaceColorUniform("uColorLeftDark", settings.colorLeftDark)
                setFaceColorUniform("uColorRightLight", settings.colorRightLight)
                setFaceColorUniform("uColorRightDark", settings.colorRightDark)

                // Uniforms de base
                shader.setFloatUniform("resolution", size.width, size.height)
                shader.setFloatUniform("uTime", time)

                // Uniforms non utilisés dans v10/v11 mais nécessaires
                shader.setFloatUniform("uSpecularPower", 32f)
                shader.setFloatUniform("uNoiseStrength", 0.005f)

                drawRect(ShaderBrush(shader))
            }

            // Debug overlay
            Text(
                text = """
                    ReactiveBox - Interactive (v11)
                    Tilt: X=${tiltX.formatDecimal(2)} Y=${tiltY.formatDecimal(2)}
                    Brightness: ${settings.faceBrightness.formatDecimal(2)}
                """.trimIndent(),
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }

        // Le Panneau de Réglages (Modal Bottom Sheet)
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color(0x32151520),
            ) {
                // Contenu scrollable du panneau
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text("Réglages Dynamiques du Shader", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Fonction utilitaire pour un Slider (Générique)
                    @Composable
                    fun SettingSlider(
                        label: String,
                        value: Float,
                        range: ClosedFloatingPointRange<Float>,
                        steps: Int = 0,
                        onValueChange: (Float) -> Unit
                    ) {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                val precision = if (steps > 0) 3 else 2
                                Text(value.formatDecimal(precision), fontWeight = FontWeight.SemiBold)
                            }
                            Slider(
                                value = value,
                                onValueChange = onValueChange,
                                valueRange = range,
                                steps = steps
                            )
                        }
                    }

                    // NOUVEAU : Fonction utilitaire pour un Slider de Teinte (HUE)
                    @Composable
                    fun HUESliderSetting(
                        label: String,
                        color: Color,
                        onColorChange: (Color) -> Unit
                    ) {
                        // 1. Obtenir les valeurs HSV de la couleur actuelle
                        val hsv = HsvColorUtils.toHsv(color)
                        // 2. La teinte (index 0) est l'état du slider
                        var currentHue by remember { mutableFloatStateOf(hsv[0]) }

                        Column(Modifier.padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyMedium)

                                // Début de la modification: Affichage Hue + Box
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Affichage de la valeur HUE
                                    Text(
                                        text = "${currentHue.roundToInt()}°",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )

                                    // Visualisation de la couleur
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(color, CircleShape)
                                            .border(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), CircleShape)
                                    )
                                }
                                // Fin de la modification

                            }
                            // Slider pour la Teinte (Hue)
                            Slider(
                                value = currentHue,
                                onValueChange = { newHue ->
                                    currentHue = newHue
                                    // 3. Reconstruire la couleur avec la nouvelle Teinte (Hue),
                                    // mais conserver la Saturation (index 1) et la Valeur (index 2) originales.
                                    val newColor = HsvColorUtils.fromHsv(newHue, hsv[1], hsv[2], color.alpha)
                                    onColorChange(newColor)
                                },
                                valueRange = 0f..360f,
                                steps = 359, // 360 valeurs pour une couleur lisse
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // --- RÉGLAGES COULEURS NÉON ---
                    Text("Couleurs Néon et Highlights (Teinte)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Divider(Modifier.padding(vertical = 4.dp))
                    HUESliderSetting(
                        label = "Couleur Néon (Ligne)",
                        color = settings.neonColor,
                        onColorChange = { settings = settings.copy(neonColor = it) }
                    )
                    HUESliderSetting(
                        label = "Couleur Highlight (Noyau)",
                        color = settings.highlightColor,
                        onColorChange = { settings = settings.copy(highlightColor = it) }
                    )

                    // --- RÉGLAGES DES FACES ---
                    Text("Couleurs des Faces (Teinte des Gradients)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                    Divider(Modifier.padding(vertical = 4.dp))

                    // Fond
                    HUESliderSetting(
                        label = "Couleur du Fond (Box)",
                        color = settings.colorBox,
                        onColorChange = { settings = settings.copy(colorBox = it) }
                    )

                    // Face du haut
                    Text("Face du Haut (Top)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    HUESliderSetting(
                        label = "Top - Clair (Extérieur)",
                        color = settings.colorTopLight,
                        onColorChange = { settings = settings.copy(colorTopLight = it) }
                    )
                    HUESliderSetting(
                        label = "Top - Sombre (Intérieur)",
                        color = settings.colorTopDark,
                        onColorChange = { settings = settings.copy(colorTopDark = it) }
                    )

                    // Face du bas
                    Text("Face du Bas (Bottom)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    HUESliderSetting(
                        label = "Bottom - Clair (Extérieur)",
                        color = settings.colorBottomLight,
                        onColorChange = { settings = settings.copy(colorBottomLight = it) }
                    )
                    HUESliderSetting(
                        label = "Bottom - Sombre (Intérieur)",
                        color = settings.colorBottomDark,
                        onColorChange = { settings = settings.copy(colorBottomDark = it) }
                    )

                    // Face Gauche
                    Text("Face Gauche (Left)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    HUESliderSetting(
                        label = "Left - Clair (Extérieur)",
                        color = settings.colorLeftLight,
                        onColorChange = { settings = settings.copy(colorLeftLight = it) }
                    )
                    HUESliderSetting(
                        label = "Left - Sombre (Intérieur)",
                        color = settings.colorLeftDark,
                        onColorChange = { settings = settings.copy(colorLeftDark = it) }
                    )

                    // Face Droite
                    Text("Face Droite (Right)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    HUESliderSetting(
                        label = "Right - Clair (Extérieur)",
                        color = settings.colorRightLight,
                        onColorChange = { settings = settings.copy(colorRightLight = it) }
                    )
                    HUESliderSetting(
                        label = "Right - Sombre (Intérieur)",
                        color = settings.colorRightDark,
                        onColorChange = { settings = settings.copy(colorRightDark = it) }
                    )


                    // --- RÉGLAGES VISUELS GÉNÉRAUX ---
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Lignes et Luminosité", style = MaterialTheme.typography.titleMedium)
                    Divider(Modifier.padding(vertical = 4.dp))
                    SettingSlider(
                        label = "Luminosité des Faces",
                        value = settings.faceBrightness,
                        range = 0.1f..3.0f,
                        onValueChange = { settings = settings.copy(faceBrightness = it) }
                    )
                    SettingSlider(
                        label = "Épaisseur des Bords (Wire)",
                        value = settings.edgeThickness,
                        range = 0.0005f..0.1f,
                        steps = 50,
                        onValueChange = { settings = settings.copy(edgeThickness = it) }
                    )
                    SettingSlider(
                        label = "Intensité du Halo Néon",
                        value = settings.rimIntensity,
                        range = 0.0f..3.5f,
                        onValueChange = { settings = settings.copy(rimIntensity = it) }
                    )

                    // --- RÉGLAGES AO ---
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Ambient Occlusion (AO)", style = MaterialTheme.typography.titleMedium)
                    Divider(Modifier.padding(vertical = 4.dp))
                    SettingSlider(
                        label = "Force de l'AO",
                        value = settings.aoStrength,
                        range = 0.0f..1.0f,
                        onValueChange = { settings = settings.copy(aoStrength = it) }
                    )
                    SettingSlider(
                        label = "Rayon de l'AO",
                        value = settings.aoRadius,
                        range = 0.01f..1.0f,
                        onValueChange = { settings = settings.copy(aoRadius = it) }
                    )

                    Spacer(modifier = Modifier.height(64.dp)) // Espace pour défiler au-dessus du bord de l'écran
                }
            }
        }
    }
}
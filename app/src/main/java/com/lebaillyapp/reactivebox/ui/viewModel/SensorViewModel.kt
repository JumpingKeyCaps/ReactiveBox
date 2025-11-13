package com.lebaillyapp.reactivebox.ui.viewModel

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.AXIS_X
import android.hardware.SensorManager.AXIS_Z
import android.util.Log
import android.view.Surface
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI

// Annotation pour ignorer le warning sur l'utilisation de getSystemService en tant que Context
// car c'est la seule façon d'accéder au SensorManager.
@SuppressLint("ServiceCast")
class SensorViewModel(private val context: Context) : ViewModel(), SensorEventListener {

    // Initialisation des services et du capteur
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // Flow pour exposer les données d'inclinaison (Offset(Roll, Pitch)) à l'interface
    private val _tilt = MutableStateFlow(Offset.Zero)
    val tilt: StateFlow<Offset> = _tilt

    // Variables pour le filtre passe-bas (lissage)
    private var lastX = 0f
    private var lastY = 0f
    private val SMOOTHING = 0.2f // Coefficient de lissage (entre 0.1 et 0.3 est optimal)

    init {
        // Enregistrement du listener du capteur dès l'initialisation du ViewModel
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        val matrix = FloatArray(9)
        val remapped = FloatArray(9)

        // 1. Conversion du vecteur de rotation en matrice de rotation
        SensorManager.getRotationMatrixFromVector(matrix, event.values)

        // 2. Remappage du système de coordonnées
        // Ceci est crucial car le capteur fournit des données par rapport à un
        // système global, et nous devons le remapper pour qu'il corresponde
        // à l'orientation Portrait de l'écran (AXIS_X et AXIS_Z sont couramment utilisés
        // pour remapper en orientation naturelle/portrait)
        SensorManager.remapCoordinateSystem(matrix, AXIS_X, AXIS_Z, remapped)

        val orientation = FloatArray(3)
        // 3. Conversion de la matrice remappée en angles d'orientation (Roll, Pitch, Yaw)
        SensorManager.getOrientation(remapped, orientation)

        // orientation[2] = Roll (inclinaison latérale, X de uTilt)
        // orientation[1] = Pitch (inclinaison avant/arrière, Y de uTilt)
        val rawX = orientation[2]
        val rawY = orientation[1]

        // 4. Application du filtre passe-bas (Low-Pass Filter)
        val tiltX = lastX + (rawX - lastX) * SMOOTHING
        val tiltY = lastY + (rawY - lastY) * SMOOTHING

        // 5. Clamping (Bornage) des valeurs
        // Limite l'amplitude du mouvement envoyé au shader.
        lastX = tiltX.coerceIn(-1.5f, 1.5f) // Environ -85° à +85° en radians
        lastY = tiltY.coerceIn(-1.5f, 1.5f)

        // Mise à jour de l'état pour les Composables
        _tilt.value = Offset(lastX, lastY)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            Log.w("ReactiveBox", "Capteur peu fiable, calibration recommandée")
        }
    }

    // 6. Nettoyage : Désenregistrement du listener lors de la destruction du ViewModel
    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
        Log.i("ReactiveBox", "Capteur désenregistré.")
    }
}
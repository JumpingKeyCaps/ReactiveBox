package com.lebaillyapp.reactivebox.ui.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.AXIS_MINUS_X
import android.hardware.SensorManager.AXIS_X
import android.hardware.SensorManager.AXIS_Y
import android.hardware.SensorManager.AXIS_Z
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@SuppressLint("ServiceCast")
class SensorViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // --- ÉLÉMENTS DE CALIBRATION ---

    // 1. Stocke les lectures brutes du capteur (après filtre)
    private val _rawTiltFiltered = MutableStateFlow(Offset.Zero)

    // 2. Stocke la lecture brute qui sera considérée comme l'origine (le "zéro")
    private val _calibrationOffset = MutableStateFlow(Offset.Zero)

    // --- LISSAGE DYNAMIQUE ---

    // 4. Coefficient de lissage (SMOOTHING) contrôlé par l'utilisateur (0.01f = très lent, 1.0f = instantané)
    private val _smoothingCoefficient = MutableStateFlow(0.2f)
    val smoothingCoefficient: StateFlow<Float> = _smoothingCoefficient

    // 3. Le flux final exposé qui est la lecture corrigée par l'offset.
    val tilt: StateFlow<Offset> = combine(_rawTiltFiltered, _calibrationOffset) { rawReading, offset ->
        // Le tilt exposé est (Lecture Brute Filtrée) - (Offset de Calibration)
        Offset(rawReading.x - offset.x, rawReading.y - offset.y)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Offset.Zero
    )

    // Variables de l'état interne du filtre
    private var lastRawX = 0f
    private var lastRawY = 0f

    init {
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            Log.e("ReactiveBox", "Capteur de Rotation Vector non disponible sur cet appareil.")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        val matrix = FloatArray(9)
        val remapped = FloatArray(9)

        SensorManager.getRotationMatrixFromVector(matrix, event.values)
        // Remappage du système de coordonnées :
        // Y devient X, et X devient -Y (standard pour Roll/Pitch en mode Portrait)
        SensorManager.remapCoordinateSystem(matrix, AXIS_Y, AXIS_MINUS_X, remapped)

        val orientation = FloatArray(3)
        SensorManager.getOrientation(remapped, orientation)

        // Assignation X: Pitch (inclinaison AV/AR)
        val rawX = -orientation[1]
        // Assignation Y: Roll (inclinaison Latérale)
        val rawY = orientation[2]

        // Récupérer la valeur actuelle du coefficient de lissage
        val currentSmoothing = _smoothingCoefficient.value

        // 5. Application du filtre passe-bas (Low-Pass Filter) sur les lectures brutes
        // Utilise le coefficient dynamique
        val filteredX = lastRawX + (rawX - lastRawX) * currentSmoothing
        val filteredY = lastRawY + (rawY - lastRawY) * currentSmoothing

        // 6. Clamping (Bornage) des valeurs
        lastRawX = filteredX.coerceIn(-1.5f, 1.5f)
        lastRawY = filteredY.coerceIn(-1.5f, 1.5f)

        // Mise à jour du flux des lectures BRUTES filtrées
        _rawTiltFiltered.value = Offset(lastRawX, lastRawY)
    }

    // --- FONCTIONS PUBLIQUES ---

    /**
     * Définit la lecture actuelle du capteur (X, Y) comme la nouvelle origine (0, 0).
     */
    fun calibrate() {
        // L'offset est défini sur la valeur BRUTE actuelle du capteur.
        _calibrationOffset.value = _rawTiltFiltered.value
        Log.d("ReactiveBox", "Zéro calibré à: ${_rawTiltFiltered.value}")
    }

    /**
     * Met à jour le coefficient de lissage utilisé par le filtre passe-bas.
     * @param newSmoothing Nouveau coefficient (doit être entre 0.01f et 1.0f).
     */
    fun setSmoothingCoefficient(newSmoothing: Float) {
        // Clamping pour s'assurer que la valeur reste dans une plage utilisable
        _smoothingCoefficient.value = newSmoothing.coerceIn(0.01f, 1.0f)
        Log.d("ReactiveBox", "Coefficient de lissage ajusté à: ${_smoothingCoefficient.value}")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            Log.w("ReactiveBox", "Capteur peu fiable.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
        Log.i("ReactiveBox", "Capteur désenregistré.")
    }
}
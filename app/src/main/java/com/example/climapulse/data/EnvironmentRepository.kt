package com.example.climapulse.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.coroutines.resume
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

// Demo controlada mientras la app no esté conectada a un sensor externo.
private const val USE_DEMO_DATA = true
private const val MAX_HISTORY = 50

/**
 * Fuente única de lecturas. Simula una API en la nube (requiere conexión) y guarda en caché
 * la última lectura para poder mostrarla sin conexión.
 */
class EnvironmentRepository(
    context: Context,
    private val dataStore: DataStore<Preferences>,
    private val connectivity: ConnectivityObserver
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val json = Json { ignoreUnknownKeys = true }
    private var phase = 0f

    val history: Flow<List<EnvironmentReading>> = dataStore.data.map { prefs ->
        prefs[HISTORY]?.let { runCatching { json.decodeFromString<List<EnvironmentReading>>(it) }.getOrNull() }
            .orEmpty()
    }

    val idealRanges: Flow<Map<Metric, IdealRange>> = dataStore.data.map { prefs ->
        Metric.entries.associateWith { metric ->
            prefs[rangeKey(metric)]?.let { runCatching { json.decodeFromString<IdealRange>(it) }.getOrNull() }
                ?: metric.defaultRange
        }
    }

    val hasHardwareSensors: Boolean
        get() = !USE_DEMO_DATA && hardwareSensors().isNotEmpty()

    suspend fun fetchReading(): EnvironmentReading {
        delay(700)
        if (!connectivity.isOnline()) throw OfflineException()
        val reading = readHardware() ?: simulatedReading()
        dataStore.edit { it[LAST_READING] = json.encodeToString(EnvironmentReading.serializer(), reading) }
        return reading
    }

    suspend fun cachedReading(): EnvironmentReading? =
        dataStore.data.first()[LAST_READING]?.let {
            runCatching { json.decodeFromString<EnvironmentReading>(it) }.getOrNull()
        }

    suspend fun saveToHistory(reading: EnvironmentReading) {
        val updated = (listOf(reading) + history.first()).take(MAX_HISTORY)
        dataStore.edit { it[HISTORY] = json.encodeToString(updated) }
    }

    suspend fun clearHistory() {
        dataStore.edit { it.remove(HISTORY) }
    }

    suspend fun saveIdealRange(metric: Metric, range: IdealRange) {
        dataStore.edit { it[rangeKey(metric)] = json.encodeToString(range) }
    }

    private fun simulatedReading(): EnvironmentReading {
        phase += 0.42f
        return EnvironmentReading(
            temperature = 22.6f + sin(phase) * 1.1f,
            humidity = 54f + cos(phase * 0.75f) * 4f,
            light = 480f + sin(phase * 0.5f) * 120f,
            timestampMillis = System.currentTimeMillis()
        )
    }

    private fun hardwareSensors(): List<Sensor> = listOfNotNull(
        sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE),
        sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY),
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    )

    private suspend fun readHardware(): EnvironmentReading? {
        if (!hasHardwareSensors) return null
        val fallback = cachedReading() ?: simulatedReading()
        val values = hardwareSensors().associate { sensor ->
            sensor.type to withTimeoutOrNull(1_500) { readOnce(sensor) }
        }
        return EnvironmentReading(
            temperature = values[Sensor.TYPE_AMBIENT_TEMPERATURE] ?: fallback.temperature,
            humidity = values[Sensor.TYPE_RELATIVE_HUMIDITY]?.coerceIn(0f, 100f) ?: fallback.humidity,
            light = values[Sensor.TYPE_LIGHT]?.coerceAtLeast(0f) ?: fallback.light,
            timestampMillis = System.currentTimeMillis(),
            usingHardware = true
        )
    }

    private suspend fun readOnce(sensor: Sensor): Float? = suspendCancellableCoroutine { continuation ->
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                sensorManager.unregisterListener(this)
                if (continuation.isActive) continuation.resume(event.values.firstOrNull())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        continuation.invokeOnCancellation { sensorManager.unregisterListener(listener) }
    }

    private companion object {
        val LAST_READING = stringPreferencesKey("last_reading")
        val HISTORY = stringPreferencesKey("history")
        fun rangeKey(metric: Metric) = stringPreferencesKey("range_${metric.name.lowercase()}")
    }
}

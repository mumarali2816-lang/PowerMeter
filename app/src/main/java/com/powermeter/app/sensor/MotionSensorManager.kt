package com.powermeter.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MotionMetrics(
    val currentIntensity: Float = 0f,
    val peakIntensity: Float = 0f,
    val repetitionCount: Int = 0,
    val cadenceRpm: Float = 0f,
    val consistencyPercentage: Float = 100f,
    val isSensorAvailable: Boolean = true
)

class MotionSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val signalProcessor = SignalProcessor()
    private val repetitionDetector = RepetitionDetector()

    private val _motionMetrics = MutableStateFlow(MotionMetrics(isSensorAvailable = accelerometer != null))
    val motionMetrics: StateFlow<MotionMetrics> = _motionMetrics.asStateFlow()

    private var isActive = false

    fun startListening(lowIntensityMode: Boolean = false) {
        if (accelerometer == null) {
            _motionMetrics.value = _motionMetrics.value.copy(isSensorAvailable = false)
            return
        }

        signalProcessor.reset()
        repetitionDetector.reset()
        repetitionDetector.setLowIntensityMode(lowIntensityMode)
        isActive = true

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopListening() {
        if (isActive) {
            sensorManager.unregisterListener(this)
            isActive = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isActive || event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val nowMs = System.currentTimeMillis()
            val filtered = signalProcessor.processSample(event.values[0], event.values[1], event.values[2])

            repetitionDetector.onNewSample(filtered.magnitude, nowMs) { count, peak, intervalMs ->
                val rpm = if (intervalMs > 0) (60_000f / intervalMs).coerceIn(0f, 180f) else 0f
                _motionMetrics.value = _motionMetrics.value.copy(
                    repetitionCount = count,
                    peakIntensity = peak,
                    cadenceRpm = rpm,
                    consistencyPercentage = repetitionDetector.calculateConsistencyPercentage()
                )
            }

            _motionMetrics.value = _motionMetrics.value.copy(
                currentIntensity = filtered.magnitude
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

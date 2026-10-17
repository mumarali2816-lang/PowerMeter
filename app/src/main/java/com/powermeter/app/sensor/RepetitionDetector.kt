package com.powermeter.app.sensor

import kotlin.math.sqrt

class RepetitionDetector(
    private var highThreshold: Float = 3.2f,
    private var lowThreshold: Float = 1.1f,
    private val minRepDurationMs: Long = 380,
    private val maxRepDurationMs: Long = 3000
) {
    enum class State {
        WAITING_FOR_PEAK,
        PEAK_DETECTED
    }

    private var currentState = State.WAITING_FOR_PEAK
    private var lastRepTimestamp: Long = 0L
    private var currentPeakIntensity: Float = 0f
    private val repIntervals = mutableListOf<Long>()

    var repetitionCount: Int = 0
        private set

    fun setLowIntensityMode(enabled: Boolean) {
        if (enabled) {
            highThreshold = 2.0f
            lowThreshold = 0.7f
        } else {
            highThreshold = 3.2f
            lowThreshold = 1.1f
        }
    }

    fun onNewSample(
        magnitude: Float,
        timestampMs: Long,
        onRepetitionCounted: (repNumber: Int, peakIntensity: Float, repIntervalMs: Long) -> Unit
    ) {
        when (currentState) {
            State.WAITING_FOR_PEAK -> {
                if (magnitude >= highThreshold) {
                    currentState = State.PEAK_DETECTED
                    currentPeakIntensity = magnitude
                }
            }

            State.PEAK_DETECTED -> {
                if (magnitude > currentPeakIntensity) {
                    currentPeakIntensity = magnitude
                }

                if (magnitude <= lowThreshold) {
                    val durationSinceLastRep = timestampMs - lastRepTimestamp
                    val isCadenceValid = lastRepTimestamp == 0L || 
                            (durationSinceLastRep >= minRepDurationMs && durationSinceLastRep <= maxRepDurationMs)

                    if (isCadenceValid) {
                        repetitionCount++
                        val interval = if (lastRepTimestamp == 0L) 800L else durationSinceLastRep
                        lastRepTimestamp = timestampMs
                        
                        if (repIntervals.size >= 10) repIntervals.removeAt(0)
                        repIntervals.add(interval)

                        onRepetitionCounted(repetitionCount, currentPeakIntensity, interval)
                    }

                    currentState = State.WAITING_FOR_PEAK
                    currentPeakIntensity = 0f
                }
            }
        }
    }

    fun calculateConsistencyPercentage(): Float {
        if (repIntervals.size < 2) return 100f
        val avg = repIntervals.average()
        val variance = repIntervals.map { (it - avg) * (it - avg) }.average()
        val stdDev = sqrt(variance)
        val cv = (stdDev / avg).toFloat()
        return ((1f - (cv / 0.8f)).coerceIn(0.1f, 1.0f)) * 100f
    }

    fun reset() {
        currentState = State.WAITING_FOR_PEAK
        lastRepTimestamp = 0L
        currentPeakIntensity = 0f
        repetitionCount = 0
        repIntervals.clear()
    }
}

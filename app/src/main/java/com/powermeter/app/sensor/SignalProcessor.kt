package com.powermeter.app.sensor

import kotlin.math.sqrt

class SignalProcessor {
    private val alpha = 0.82f
    private val gravity = FloatArray(3) { 0f }
    private var isInitialized = false

    private val windowSize = 5
    private val magnitudeHistory = FloatArray(windowSize) { 0f }
    private var historyIndex = 0
    private var historyCount = 0

    fun processSample(rawX: Float, rawY: Float, rawZ: Float): FilteredMotionData {
        if (!isInitialized) {
            gravity[0] = rawX
            gravity[1] = rawY
            gravity[2] = rawZ
            isInitialized = true
        } else {
            gravity[0] = alpha * gravity[0] + (1 - alpha) * rawX
            gravity[1] = alpha * gravity[1] + (1 - alpha) * rawY
            gravity[2] = alpha * gravity[2] + (1 - alpha) * rawZ
        }

        val linearX = rawX - gravity[0]
        val linearY = rawY - gravity[1]
        val linearZ = rawZ - gravity[2]

        val dynamicMagnitude = sqrt((linearX * linearX + linearY * linearY + linearZ * linearZ).toDouble()).toFloat()

        magnitudeHistory[historyIndex] = dynamicMagnitude
        historyIndex = (historyIndex + 1) % windowSize
        if (historyCount < windowSize) historyCount++

        var smoothedMagnitude = 0f
        for (i in 0 until historyCount) {
            smoothedMagnitude += magnitudeHistory[i]
        }
        smoothedMagnitude /= historyCount

        return FilteredMotionData(
            linearX = linearX,
            linearY = linearY,
            linearZ = linearZ,
            magnitude = smoothedMagnitude,
            rawMagnitude = sqrt((rawX * rawX + rawY * rawY + rawZ * rawZ).toDouble()).toFloat()
        )
    }

    fun reset() {
        isInitialized = false
        historyIndex = 0
        historyCount = 0
        for (i in 0 until 3) gravity[i] = 0f
        for (i in 0 until windowSize) magnitudeHistory[i] = 0f
    }
}

data class FilteredMotionData(
    val linearX: Float,
    val linearY: Float,
    val linearZ: Float,
    val magnitude: Float,
    val rawMagnitude: Float
)

package com.powermeter.app.scoring

import kotlin.math.roundToInt

object ScoringEngine {

    fun calculateScore(
        repetitions: Int,
        averageIntensity: Float,
        cadenceRpm: Float,
        consistencyPercentage: Float,
        durationSeconds: Int
    ): Int {
        if (repetitions == 0) return 0

        val baseRepPoints = repetitions * 100f
        val intensityBonus = (averageIntensity.coerceIn(0f, 25f)) * 18f
        val cadenceBonus = (cadenceRpm.coerceIn(0f, 160f)) * 2.5f
        val multiplier = (consistencyPercentage / 100f).coerceIn(0.5f, 1.25f)

        val totalRawScore = (baseRepPoints + intensityBonus + cadenceBonus) * multiplier

        val durationWeight = when (durationSeconds) {
            15 -> 0.6f
            30 -> 1.0f
            60 -> 1.8f
            120 -> 3.2f
            else -> durationSeconds / 30f
        }

        return (totalRawScore * durationWeight).roundToInt().coerceAtLeast(0)
    }

    fun calculateExpGained(score: Int): Int = (score * 0.15f).roundToInt()

    fun getLevelFromExp(totalExp: Int): UserLevel {
        val level = (totalExp / 1000) + 1
        val currentLevelExp = totalExp % 1000
        val badge = when {
            level >= 25 -> "Champion"
            level >= 15 -> "Expert"
            level >= 8 -> "Advanced"
            level >= 3 -> "Active"
            else -> "Beginner"
        }
        return UserLevel(
            levelNumber = level,
            currentLevelExp = currentLevelExp,
            expForNextLevel = 1000,
            progress = currentLevelExp.toFloat() / 1000f,
            title = badge
        )
    }
}

data class UserLevel(
    val levelNumber: Int,
    val currentLevelExp: Int,
    val expForNextLevel: Int,
    val progress: Float,
    val title: String
)

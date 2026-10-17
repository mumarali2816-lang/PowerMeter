package com.powermeter.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenge_history")
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int,
    val repetitionCount: Int,
    val score: Int,
    val peakIntensity: Float,
    val avgCadenceRpm: Float,
    val consistencyPercentage: Float,
    val expEarned: Int
)

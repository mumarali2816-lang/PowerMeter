package com.powermeter.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: ChallengeEntity): Long

    @Query("SELECT * FROM challenge_history ORDER BY timestamp DESC")
    fun getAllChallenges(): Flow<List<ChallengeEntity>>

    @Query("SELECT MAX(score) FROM challenge_history")
    fun getPersonalRecordScore(): Flow<Int?>

    @Query("SELECT SUM(score) FROM challenge_history")
    fun getTotalLifetimePoints(): Flow<Int?>

    @Query("SELECT SUM(repetitionCount) FROM challenge_history")
    fun getTotalLifetimeReps(): Flow<Int?>

    @Query("DELETE FROM challenge_history")
    suspend fun clearAllHistory()
}

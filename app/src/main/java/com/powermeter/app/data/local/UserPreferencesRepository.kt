package com.powermeter.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {

    private val KEY_NICKNAME = stringPreferencesKey("nickname")
    private val KEY_SAFETY_ACCEPTED = booleanPreferencesKey("safety_accepted")
    private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    private val KEY_HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
    private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
    private val KEY_LOW_INTENSITY = booleanPreferencesKey("low_intensity")
    private val KEY_CHALLENGE_DURATION = intPreferencesKey("challenge_duration")

    val nickname: Flow<String> = context.dataStore.data.map { it[KEY_NICKNAME] ?: "Athlete" }
    val isSafetyAccepted: Flow<Boolean> = context.dataStore.data.map { it[KEY_SAFETY_ACCEPTED] ?: false }
    val isSoundEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_SOUND_ENABLED] ?: true }
    val isHapticEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_HAPTIC_ENABLED] ?: true }
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_DARK_MODE] ?: true }
    val isLowIntensity: Flow<Boolean> = context.dataStore.data.map { it[KEY_LOW_INTENSITY] ?: false }
    val selectedDuration: Flow<Int> = context.dataStore.data.map { it[KEY_CHALLENGE_DURATION] ?: 30 }

    suspend fun setNickname(name: String) = context.dataStore.edit { it[KEY_NICKNAME] = name }
    suspend fun setSafetyAccepted(accepted: Boolean) = context.dataStore.edit { it[KEY_SAFETY_ACCEPTED] = accepted }
    suspend fun setSoundEnabled(enabled: Boolean) = context.dataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    suspend fun setHapticEnabled(enabled: Boolean) = context.dataStore.edit { it[KEY_HAPTIC_ENABLED] = enabled }
    suspend fun setDarkMode(enabled: Boolean) = context.dataStore.edit { it[KEY_DARK_MODE] = enabled }
    suspend fun setLowIntensity(enabled: Boolean) = context.dataStore.edit { it[KEY_LOW_INTENSITY] = enabled }
    suspend fun setSelectedDuration(duration: Int) = context.dataStore.edit { it[KEY_CHALLENGE_DURATION] = duration }
}

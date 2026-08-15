package com.hyper.phone.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val AUTO_RECORD = booleanPreferencesKey("auto_record")
        val REDIAL_DELAY = floatPreferencesKey("redial_delay")
        val REDIAL_ATTEMPTS = floatPreferencesKey("redial_attempts")
        val AUTO_ANSWER = booleanPreferencesKey("auto_answer")
        val FLIP_TO_MUTE = booleanPreferencesKey("flip_to_mute")
        val RAISE_TO_ANSWER = booleanPreferencesKey("raise_to_answer")
        val SHAKE_TO_REJECT = booleanPreferencesKey("shake_to_reject")
        
        val BLOCK_NON_CONTACTS = booleanPreferencesKey("block_non_contacts")
        val BLOCK_PRIVATE = booleanPreferencesKey("block_private")
        val MINIMIZE_INCOMING = booleanPreferencesKey("minimize_incoming")
        
        val TTS_ANNOUNCER = booleanPreferencesKey("tts_announcer")
        val PREFIX_ROUTING = booleanPreferencesKey("prefix_routing")
        val APP_LOCK = booleanPreferencesKey("app_lock")
    }

    val autoRecordFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTO_RECORD] ?: false }
    val redialDelayFlow: Flow<Float> = context.dataStore.data.map { it[REDIAL_DELAY] ?: 5f }
    val redialAttemptsFlow: Flow<Float> = context.dataStore.data.map { it[REDIAL_ATTEMPTS] ?: 3f }
    val autoAnswerFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTO_ANSWER] ?: false }
    val flipToMuteFlow: Flow<Boolean> = context.dataStore.data.map { it[FLIP_TO_MUTE] ?: true }
    val raiseToAnswerFlow: Flow<Boolean> = context.dataStore.data.map { it[RAISE_TO_ANSWER] ?: false }
    val shakeToRejectFlow: Flow<Boolean> = context.dataStore.data.map { it[SHAKE_TO_REJECT] ?: false }
    
    val blockNonContactsFlow: Flow<Boolean> = context.dataStore.data.map { it[BLOCK_NON_CONTACTS] ?: false }
    val blockPrivateFlow: Flow<Boolean> = context.dataStore.data.map { it[BLOCK_PRIVATE] ?: false }
    val minimizeIncomingFlow: Flow<Boolean> = context.dataStore.data.map { it[MINIMIZE_INCOMING] ?: false }

    val ttsAnnouncerFlow: Flow<Boolean> = context.dataStore.data.map { it[TTS_ANNOUNCER] ?: false }
    val prefixRoutingFlow: Flow<Boolean> = context.dataStore.data.map { it[PREFIX_ROUTING] ?: false }
    val appLockFlow: Flow<Boolean> = context.dataStore.data.map { it[APP_LOCK] ?: false }

    suspend fun saveAutoRecord(value: Boolean) { context.dataStore.edit { it[AUTO_RECORD] = value } }
    suspend fun saveRedialDelay(value: Float) { context.dataStore.edit { it[REDIAL_DELAY] = value } }
    suspend fun saveRedialAttempts(value: Float) { context.dataStore.edit { it[REDIAL_ATTEMPTS] = value } }
    suspend fun saveAutoAnswer(value: Boolean) { context.dataStore.edit { it[AUTO_ANSWER] = value } }
    suspend fun saveFlipToMute(value: Boolean) { context.dataStore.edit { it[FLIP_TO_MUTE] = value } }
    suspend fun saveRaiseToAnswer(value: Boolean) { context.dataStore.edit { it[RAISE_TO_ANSWER] = value } }
    suspend fun saveShakeToReject(value: Boolean) { context.dataStore.edit { it[SHAKE_TO_REJECT] = value } }
    
    suspend fun saveBlockNonContacts(value: Boolean) { context.dataStore.edit { it[BLOCK_NON_CONTACTS] = value } }
    suspend fun saveBlockPrivate(value: Boolean) { context.dataStore.edit { it[BLOCK_PRIVATE] = value } }
    suspend fun saveMinimizeIncoming(value: Boolean) { context.dataStore.edit { it[MINIMIZE_INCOMING] = value } }

    suspend fun saveTtsAnnouncer(value: Boolean) { context.dataStore.edit { it[TTS_ANNOUNCER] = value } }
    suspend fun savePrefixRouting(value: Boolean) { context.dataStore.edit { it[PREFIX_ROUTING] = value } }
    suspend fun saveAppLock(value: Boolean) { context.dataStore.edit { it[APP_LOCK] = value } }
}

package com.example.climapulse.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Destino que decide el Router detrás de la Splash Screen. */
sealed interface StartDestination {
    data object Onboarding : StartDestination
    data class Login(val biometricUnlock: Boolean) : StartDestination
    data object Home : StartDestination
}

/**
 * Sesión, onboarding, perfil y preferencias. El backend de autenticación es simulado:
 * acepta cualquier correo válido con contraseña de 6+ caracteres y emite un token aleatorio.
 */
class SessionRepository(
    private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher
) {
    val profile: Flow<UserProfile> = dataStore.data.map {
        UserProfile(
            name = it[USER_NAME].orEmpty(),
            email = it[USER_EMAIL].orEmpty(),
            phone = it[USER_PHONE].orEmpty()
        )
    }
    val biometricEnabled: Flow<Boolean> = dataStore.data.map { it[BIOMETRIC_ENABLED] ?: false }
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: false }
    val autoRefreshEnabled: Flow<Boolean> = dataStore.data.map { it[AUTO_REFRESH_ENABLED] ?: true }

    /** Bandera del tour guiado del Home: se muestra una sola vez (o cuando el usuario lo pide). */
    val tourDone: Flow<Boolean> = dataStore.data.map { it[TOUR_DONE] ?: false }

    suspend fun resolveStartDestination(): StartDestination {
        val prefs = dataStore.data.first()
        if (prefs[ONBOARDING_DONE] != true) return StartDestination.Onboarding
        val token = prefs[AUTH_TOKEN]?.let(cipher::decrypt)
        val expiry = prefs[TOKEN_EXPIRY] ?: 0L
        val validSession = token != null && expiry > System.currentTimeMillis()
        return when {
            !validSession -> StartDestination.Login(biometricUnlock = false)
            prefs[BIOMETRIC_ENABLED] == true -> StartDestination.Login(biometricUnlock = true)
            else -> StartDestination.Home
        }
    }

    suspend fun completeOnboarding(notificationsGranted: Boolean) {
        dataStore.edit {
            it[ONBOARDING_DONE] = true
            it[NOTIFICATIONS_ENABLED] = notificationsGranted
        }
    }

    suspend fun login(email: String, password: String) {
        delay(NETWORK_DELAY)
        if (password.length < 6) throw AuthException("El correo o la contraseña no coinciden.")
        val savedName = dataStore.data.first()[USER_NAME]
        storeSession(email = email, name = savedName ?: email.substringBefore('@'))
    }

    suspend fun loginWithProvider(provider: String) {
        delay(NETWORK_DELAY)
        storeSession(email = "usuario@${provider.lowercase()}.com", name = "Usuario de $provider")
    }

    suspend fun register(name: String, email: String, phone: String, password: String) {
        delay(NETWORK_DELAY)
        if (password.length < 6) throw AuthException("La contraseña debe tener al menos 6 caracteres.")
        storeSession(email = email, name = name, phone = phone)
    }

    suspend fun requestPasswordReset(@Suppress("UNUSED_PARAMETER") email: String) {
        delay(NETWORK_DELAY)
    }

    /** Llamado tras una autenticación biométrica exitosa: renueva la vigencia del token. */
    suspend fun unlockSession(): Boolean {
        val prefs = dataStore.data.first()
        if (prefs[AUTH_TOKEN]?.let(cipher::decrypt) == null) return false
        dataStore.edit { it[TOKEN_EXPIRY] = System.currentTimeMillis() + SESSION_DURATION }
        return true
    }

    suspend fun hasStoredSession(): Boolean =
        dataStore.data.first()[AUTH_TOKEN]?.let(cipher::decrypt) != null

    suspend fun logout() {
        dataStore.edit {
            it.remove(AUTH_TOKEN)
            it.remove(TOKEN_EXPIRY)
            it[BIOMETRIC_ENABLED] = false
        }
    }

    suspend fun updateProfile(profile: UserProfile) {
        delay(400)
        dataStore.edit {
            it[USER_NAME] = profile.name
            it[USER_EMAIL] = profile.email
            it[USER_PHONE] = profile.phone
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) = dataStore.edit { it[BIOMETRIC_ENABLED] = enabled }
    suspend fun setNotificationsEnabled(enabled: Boolean) = dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    suspend fun setAutoRefreshEnabled(enabled: Boolean) = dataStore.edit { it[AUTO_REFRESH_ENABLED] = enabled }
    suspend fun setTourDone(done: Boolean) = dataStore.edit { it[TOUR_DONE] = done }

    private suspend fun storeSession(email: String, name: String, phone: String? = null) {
        val token = UUID.randomUUID().toString()
        dataStore.edit {
            it[AUTH_TOKEN] = cipher.encrypt(token)
            it[TOKEN_EXPIRY] = System.currentTimeMillis() + SESSION_DURATION
            it[USER_EMAIL] = email
            it[USER_NAME] = name
            if (phone != null) it[USER_PHONE] = phone
        }
    }

    private companion object {
        const val NETWORK_DELAY = 900L
        val SESSION_DURATION = TimeUnit.DAYS.toMillis(7)

        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val AUTH_TOKEN = stringPreferencesKey("auth_token_encrypted")
        val TOKEN_EXPIRY = longPreferencesKey("auth_token_expiry")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_PHONE = stringPreferencesKey("user_phone")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val AUTO_REFRESH_ENABLED = booleanPreferencesKey("auto_refresh_enabled")
        val TOUR_DONE = booleanPreferencesKey("home_tour_done")
    }
}

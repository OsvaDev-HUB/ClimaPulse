package com.example.climapulse

import android.app.Application
import com.example.climapulse.data.ConnectivityObserver
import com.example.climapulse.data.EnvironmentRepository
import com.example.climapulse.data.SessionRepository
import com.example.climapulse.data.TokenCipher
import com.example.climapulse.data.appDataStore

class ClimaPulseApplication : Application() {
    val container by lazy { AppContainer(this) }
}

/** Contenedor de dependencias manual compartido por los ViewModels. */
class AppContainer(application: Application) {
    val connectivity = ConnectivityObserver(application)
    val sessionRepository = SessionRepository(application.appDataStore, TokenCipher())
    val environmentRepository = EnvironmentRepository(application, application.appDataStore, connectivity)
}

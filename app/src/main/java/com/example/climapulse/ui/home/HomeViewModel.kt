package com.example.climapulse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climapulse.data.ConnectivityObserver
import com.example.climapulse.data.EnvironmentReading
import com.example.climapulse.data.EnvironmentRepository
import com.example.climapulse.data.IdealRange
import com.example.climapulse.data.Metric
import com.example.climapulse.data.SessionRepository
import com.example.climapulse.ui.UiState
import com.example.climapulse.ui.appViewModelFactory
import com.example.climapulse.ui.toUserMessage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardData(
    val reading: EnvironmentReading,
    val ranges: Map<Metric, IdealRange>,
    val trend: List<EnvironmentReading>
)

/** Compartido por Inicio, Sensor y Reportes (vive mientras el Home esté en la pila). */
class HomeViewModel(
    private val environment: EnvironmentRepository,
    private val session: SessionRepository,
    connectivity: ConnectivityObserver
) : ViewModel() {
    private val readingState = MutableStateFlow<UiState<EnvironmentReading>>(UiState.Loading)
    private val _isRefreshing = MutableStateFlow(false)
    private val _messages = Channel<String>(Channel.BUFFERED)
    private var loadJob: Job? = null

    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val messages = _messages.receiveAsFlow()
    val hasHardwareSensors: Boolean get() = environment.hasHardwareSensors

    // Arranca en true para no lanzar el tour antes de leer la bandera real.
    val tourDone: StateFlow<Boolean> = session.tourDone
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isOnline: StateFlow<Boolean> = connectivity.isOnlineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val uiState: StateFlow<UiState<DashboardData>> = combine(
        readingState,
        environment.idealRanges,
        environment.history
    ) { reading, ranges, history ->
        when (reading) {
            is UiState.Success -> UiState.Success(
                DashboardData(reading.data, ranges, history.take(6).reversed()),
                fromCache = reading.fromCache
            )
            is UiState.Error -> reading
            UiState.Empty -> UiState.Empty
            UiState.Loading -> UiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init {
        load(showSkeleton = true, announce = false)
        viewModelScope.launch {
            session.autoRefreshEnabled.collectLatest { enabled ->
                while (enabled) {
                    delay(AUTO_REFRESH_MILLIS)
                    load(showSkeleton = false, announce = false)
                }
            }
        }
        // Al recuperar la conexión se sincroniza solo, sin que el usuario tenga que reintentar.
        viewModelScope.launch {
            connectivity.isOnlineFlow.drop(1).filter { it }.collect {
                val current = readingState.value
                if (current is UiState.Error || (current as? UiState.Success)?.fromCache == true) {
                    load(showSkeleton = current is UiState.Error, announce = false)
                }
            }
        }
    }

    fun retry() = load(showSkeleton = true, announce = false)

    fun refresh(successMessage: String = "Lecturas actualizadas") =
        load(showSkeleton = false, announce = true, successMessage = successMessage)

    fun onNotificationPermission(granted: Boolean) {
        viewModelScope.launch { session.setNotificationsEnabled(granted) }
    }

    fun finishTour() {
        viewModelScope.launch { session.setTourDone(true) }
    }

    fun replayTour() {
        viewModelScope.launch { session.setTourDone(false) }
    }

    fun saveMeasurement() {
        val reading = (readingState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            environment.saveToHistory(reading)
            _messages.send("Medición guardada en el historial")
        }
    }

    private fun load(showSkeleton: Boolean, announce: Boolean, successMessage: String = "") {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isRefreshing.value = !showSkeleton
            if (showSkeleton) readingState.value = UiState.Loading
            try {
                readingState.value = UiState.Success(environment.fetchReading())
                if (announce) _messages.send(successMessage)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Caché: si hay una lectura previa la mostramos en vez de dejar la pantalla en blanco.
                val cached = (readingState.value as? UiState.Success)?.data ?: environment.cachedReading()
                if (cached != null) {
                    readingState.value = UiState.Success(cached, fromCache = true)
                    if (announce) _messages.send(e.toUserMessage())
                } else {
                    readingState.value = UiState.Error(e.toUserMessage())
                }
            } finally {
                // Un job cancelado por una recarga más nueva no debe apagar su indicador.
                if (isActive) _isRefreshing.value = false
            }
        }
    }

    companion object {
        const val AUTO_REFRESH_MILLIS = 30_000L

        val Factory = appViewModelFactory { c, _ ->
            HomeViewModel(c.environmentRepository, c.sessionRepository, c.connectivity)
        }
    }
}

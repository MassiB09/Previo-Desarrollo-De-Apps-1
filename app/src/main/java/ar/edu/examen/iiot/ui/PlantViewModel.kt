package ar.edu.examen.iiot.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ar.edu.examen.iiot.data.PlantRepository
import ar.edu.examen.iiot.model.PlantRules
import ar.edu.examen.iiot.model.Reading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardState(
    val readings: List<Reading> = emptyList(),
    val filter: String = "",
    val loading: Boolean = false,
    val syncMessage: String = "Lecturas locales",
    val notification: String? = null,
    val threshold: Double = PlantRules.DEFAULT_TEMPERATURE_LIMIT,
    val alertCount: Int = 0,
    val thresholdDialogVisible: Boolean = false,
    val thresholdInput: String = "",
    val thresholdError: String? = null,
    val historyMachineId: String? = null,
    val historyMachineName: String? = null,
    val historyReadings: List<Reading> = emptyList()
)

class PlantViewModel(private val repository: PlantRepository) : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        val threshold = repository.currentThreshold()
        val data = repository.localReadings()
        setReadings(data, threshold)
    }

    private fun setReadings(items: List<Reading>, threshold: Double = _state.value.threshold) {
        _state.update { current ->
            current.copy(
                readings = items,
                threshold = threshold,
                alertCount = items.count { PlantRules.requiresAttention(it, threshold) }
            )
        }
    }

    fun refresh() {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, notification = null) }
            try {
                repository.sync()
                val items = repository.search(_state.value.filter)
                setReadings(items, _state.value.threshold)
                val historyRows = _state.value.historyMachineId?.let(repository::history)
                if (historyRows != null) {
                    _state.update { it.copy(historyReadings = historyRows) }
                }
                _state.update { it.copy(syncMessage = repository.lastSyncMessage) }
            } catch (exception: Exception) {
                _state.update { it.copy(syncMessage = "No se pudo actualizar la telemetría") }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun filter(text: String) {
        _state.update { it.copy(filter = text) }
        try {
            setReadings(repository.search(text), _state.value.threshold)
        } catch (exception: Exception) {
            _state.update {
                it.copy(notification = "La búsqueda no se pudo completar")
            }
        }
    }

    fun configureThreshold() {
        _state.update { current ->
            current.copy(
                thresholdDialogVisible = true,
                thresholdInput = formatThreshold(current.threshold),
                thresholdError = null
            )
        }
    }

    fun updateThresholdInput(text: String) {
        _state.update { it.copy(thresholdInput = text, thresholdError = null) }
    }

    fun saveThreshold() {
        val input = _state.value.thresholdInput.trim().replace(',', '.')
        val value = input.toDoubleOrNull()
        val error = when {
            input.isEmpty() -> "Ingresá un valor numérico entre 30 y 120 °C"
            value == null -> "El valor debe ser numérico"
            !PlantRules.isValidThreshold(value) -> "El umbral debe estar entre 30 y 120 °C"
            else -> null
        }
        if (error != null || value == null) {
            _state.update { it.copy(thresholdError = error) }
            return
        }
        repository.saveThreshold(value)
        _state.update { current ->
            current.copy(
                threshold = value,
                alertCount = current.readings.count {
                    PlantRules.requiresAttention(it, value)
                },
                thresholdDialogVisible = false,
                thresholdInput = "",
                thresholdError = null
            )
        }
    }

    fun dismissThresholdDialog() {
        _state.update {
            it.copy(
                thresholdDialogVisible = false,
                thresholdInput = "",
                thresholdError = null
            )
        }
    }

    private fun formatThreshold(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

    fun showHistory(machineId: String) {
        try {
            val readings = repository.history(machineId)
            val machineName = _state.value.readings
                .firstOrNull { it.machineId == machineId }
                ?.machineName
                ?: readings.firstOrNull()?.machineName
                ?: machineId
            _state.update {
                it.copy(
                    historyMachineId = machineId,
                    historyMachineName = machineName,
                    historyReadings = readings,
                    notification = null
                )
            }
        } catch (exception: Exception) {
            _state.update {
                it.copy(notification = "No se pudo cargar el historial")
            }
        }
    }

    fun closeHistory() {
        _state.update {
            it.copy(
                historyMachineId = null,
                historyMachineName = null,
                historyReadings = emptyList()
            )
        }
    }

    fun dismissNotification() {
        _state.update { it.copy(notification = null) }
    }

    class Factory(private val repository: PlantRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PlantViewModel::class.java))
            return PlantViewModel(repository) as T
        }
    }
}

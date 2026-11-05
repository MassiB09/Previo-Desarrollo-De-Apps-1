package ar.edu.examen.iiot.model

data class Reading(
    val id: Long = 0,
    val machineId: String,
    val machineName: String,
    val temperature: Double,
    val humidity: Double,
    val vibration: Double,
    val timestamp: Long,
    val source: String
)

object PlantRules {
    const val MIN_TEMPERATURE_LIMIT = 30.0
    const val MAX_TEMPERATURE_LIMIT = 120.0
    const val DEFAULT_TEMPERATURE_LIMIT = 65.0

    fun requiresAttention(reading: Reading): Boolean =
        requiresAttention(reading, DEFAULT_TEMPERATURE_LIMIT)

    fun requiresAttention(reading: Reading, threshold: Double): Boolean =
        reading.temperature >= threshold

    fun isValidThreshold(value: Double): Boolean =
        value.isFinite() && value in MIN_TEMPERATURE_LIMIT..MAX_TEMPERATURE_LIMIT
}

package ar.edu.examen.iiot

import ar.edu.examen.iiot.model.PlantRules
import ar.edu.examen.iiot.model.Reading
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantRulesTest {
    private fun sample(temp: Double) = Reading(
        machineId = "TEST-01",
        machineName = "Equipo de prueba",
        temperature = temp,
        humidity = 50.0,
        vibration = 1.0,
        timestamp = 1L,
        source = "Test"
    )

    @Test fun belowThresholdIsNormal() {
        assertFalse(PlantRules.requiresAttention(sample(64.9)))
    }

    @Test fun thresholdIsAlert() {
        assertTrue(PlantRules.requiresAttention(sample(65.0)))
    }

    @Test fun customThresholdIsRespected() {
        assertTrue(PlantRules.requiresAttention(sample(70.0), 70.0))
        assertFalse(PlantRules.requiresAttention(sample(69.9), 70.0))
    }

    @Test fun thresholdValidationAcceptsBounds() {
        assertTrue(PlantRules.isValidThreshold(30.0))
        assertTrue(PlantRules.isValidThreshold(120.0))
    }

    @Test fun thresholdValidationRejectsOutOfRange() {
        assertFalse(PlantRules.isValidThreshold(29.9))
        assertFalse(PlantRules.isValidThreshold(120.1))
        assertFalse(PlantRules.isValidThreshold(Double.NaN))
    }
}

package ar.edu.examen.iiot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ar.edu.examen.iiot.model.PlantRules
import ar.edu.examen.iiot.model.Reading
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    state: DashboardState,
    onFilterChanged: (String) -> Unit,
    onThresholdClick: () -> Unit,
    onThresholdInputChanged: (String) -> Unit,
    onSaveThreshold: () -> Unit,
    onDismissThresholdDialog: () -> Unit,
    onHistoryClick: (String) -> Unit,
    onDismissNotification: () -> Unit
) {
    MaterialTheme {
        if (state.thresholdDialogVisible) {
            ThresholdDialog(
                input = state.thresholdInput,
                error = state.thresholdError,
                onInputChanged = onThresholdInputChanged,
                onSave = onSaveThreshold,
                onDismiss = onDismissThresholdDialog
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Equipos monitoreados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Alertas: ${state.alertCount}",
                    color = if (state.alertCount > 0) Color(0xFFB3261E) else Color(0xFF256D41),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.filter,
                onValueChange = onFilterChanged,
                label = { Text("Buscar equipo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = onThresholdClick) {
                    Text("Umbral: ${formatThreshold(state.threshold)} °C")
                }
                if (state.loading) {
                    Text("Sincronizando...", modifier = Modifier.padding(top = 12.dp))
                }
            }
            if (state.notification != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = Color(0xFFFFF3D9),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(state.notification, color = Color(0xFF5F4500))
                        Button(onClick = onDismissNotification) { Text("Cerrar aviso") }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (state.readings.isEmpty()) {
                Text(
                    "No hay equipos para mostrar.",
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.readings, key = { it.machineId }) { reading ->
                    EquipmentCard(reading, state.threshold, onHistoryClick)
                }
                item {
                    Text(
                        "Datos de demostración: ThingSpeak aporta una medición " +
                            "pública al Compresor A; el resto es simulado. " +
                            "Los sensores físicos corresponden al teléfono.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EquipmentCard(
    reading: Reading,
    threshold: Double,
    onHistoryClick: (String) -> Unit
) {
    val alert = PlantRules.requiresAttention(reading, threshold)
    val time = SimpleDateFormat("dd/MM HH:mm", Locale("es", "AR"))
        .format(Date(reading.timestamp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(reading.machineName, fontWeight = FontWeight.Bold)
                    Text(reading.machineId, style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    if (alert) "ALERTA" else "NORMAL",
                    color = if (alert) Color(0xFFB3261E) else Color(0xFF238147),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${"%.1f".format(reading.temperature)} °C",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Humedad ${"%.0f".format(reading.humidity)} %  ·  " +
                    "Vibración ${"%.1f".format(reading.vibration)} mm/s",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "$time · ${reading.source}",
                color = Color(0xFF64748B),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            OutlinedButton(
                onClick = { onHistoryClick(reading.machineId) },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Ver historial")
            }
        }
    }
}

@Composable
private fun ThresholdDialog(
    input: String,
    error: String?,
    onInputChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Umbral de alarma") },
        text = {
            Column {
                Text("Ingresá un valor entre 30 y 120 °C.")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChanged,
                    label = { Text("Temperatura (°C)") },
                    singleLine = true,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun formatThreshold(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

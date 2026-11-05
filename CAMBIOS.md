# Cambios realizados

## Funcionalidad 1: umbral de alarma

**Validación:** se realiza en `PlantViewModel.saveThreshold()`. Se normaliza la entrada (se recorta el texto y se acepta coma decimal), se convierte con `toDoubleOrNull()` y se valida con `PlantRules.isValidThreshold()`, que exige un número finito dentro de `30.0..120.0`. Un valor vacío, no numérico, `NaN` o fuera de rango no se guarda: el diálogo permanece abierto y muestra el mensaje correspondiente mediante `DashboardState.thresholdError`.

**Persistencia:** se reutiliza la tabla SQLite `configuration`, que ya existía en `PlantDatabase`, con la clave `temperature_threshold`. `PlantDatabase.configuration()` lee con consulta parametrizada y `saveConfiguration()` escribe con `INSERT OR REPLACE`. `PlantRepository.currentThreshold()` recupera el valor al iniciar y devuelve 65 °C únicamente si no existe o no es válido; `saveThreshold()` lo persiste. El umbral sobrevive al cierre y reapertura de la aplicación.

**Actualización de la pantalla:** al guardar, el ViewModel actualiza `threshold` en `DashboardState` y recalcula `alertCount` sobre las lecturas ya cargadas usando `PlantRules.requiresAttention(reading, threshold)`, cuya condición es `temperatura >= umbral`. `DashboardScreen` observa el `StateFlow`, por lo que Compose se recompone y cada `EquipmentCard` vuelve a mostrar NORMAL o ALERTA con el nuevo valor. No se vuelve a sincronizar con la API ni se recarga la pantalla.

## Funcionalidad 2: historial

**Dónde se consulta:** `PlantDatabase.history(machineId)` ejecuta una consulta parametrizada sobre la tabla `readings`: `WHERE machine_id = ? ORDER BY captured_at DESC, id DESC LIMIT 10`. El `machine_id` se pasa como argumento, no se concatena en el SQL, y el desempate por `id` garantiza un orden estable cuando dos lecturas comparten `captured_at`. `PlantRepository.history(machineId)` encapsula esa consulta para que el ViewModel no acceda directamente a SQLite.

**Cómo se muestra:** `PlantViewModel.showHistory(machineId)` consulta el Repository y guarda `historyMachineId`, `historyMachineName` y `historyReadings` en `DashboardState`. `MainActivity` observa ese estado y, cuando `historyMachineId` no es nulo, renderiza `HistoryScreen` en lugar de `DashboardScreen`. La pantalla muestra el nombre del equipo, un botón "Volver" y una fila por lectura con fecha y hora (`dd/MM/yyyy HH:mm:ss`), temperatura y origen. Si la consulta no devuelve filas, muestra "No hay lecturas registradas para este equipo." `closeHistory()` limpia solo el estado del historial; la búsqueda, las lecturas del panel, el umbral y el contador de alertas se conservan. Si se sincroniza mientras el historial está abierto, `refresh()` vuelve a consultar el historial para reflejar las lecturas nuevas.

## Problema 1: filtro de búsqueda concatenado en SQL

- **Problema original:** `PlantDatabase.findLatestByName()` insertaba el texto del buscador directamente dentro de la consulta `LIKE`. Una comilla podía provocar un error y una entrada construida para modificar la expresión podía alterar el filtro.
- **Ubicación:** `app/src/main/java/ar/edu/examen/iiot/data/PlantDatabase.kt`, método `findLatestByName()`.
- **Por qué era un problema real:** el filtro proviene de la entrada del usuario y se interpretaba como parte de la consulta, en vez de tratarse únicamente como un valor.
- **Componentes modificados:** `PlantDatabase.findLatestByName()`.
- **Solución:** el patrón de búsqueda se entrega ahora como argumento de una consulta parametrizada. Se conserva la búsqueda parcial por nombre.
- **Justificación técnica:** SQLite distingue el texto ingresado de la estructura SQL, incluso cuando contiene comillas o caracteres de control.
- **Riesgo reducido:** inyección SQL, errores de consulta y resultados inesperados.

## Problema 2: retención del contexto de la Activity

- **Problema original:** `MainActivity` entregaba su instancia a `PlantRepository`, que la conservaba. El ViewModel puede durar más que una instancia de la Activity durante cambios de configuración.
- **Ubicación:** creación del Repository en `MainActivity.kt` y constructor de `PlantRepository.kt`.
- **Por qué era un problema real:** un objeto asociado a la interfaz podía seguir retenido después de que la Activity dejara de estar vigente.
- **Componentes modificados:** `MainActivity` y `PlantRepository`.
- **Solución:** `MainActivity` pasa el contexto de aplicación y `PlantRepository` construye `PlantDatabase` usando `applicationContext`, sin guardar el contexto recibido como propiedad.
- **Justificación técnica:** el contexto de aplicación comparte el ciclo de vida del proceso y es apropiado para un Repository retenido por un ViewModel.
- **Riesgo reducido:** fugas de memoria por retener instancias antiguas de la Activity.

## Validación

### Prueba automatizada reproducible

Desde la raíz de `padn05`, en PowerShell con JDK 17:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --no-daemon --console=plain
```

**Resultado observado:** `BUILD SUCCESSFUL`; 5 tests, 0 fallas y 0 errores. La suite existente incluye `belowThresholdIsNormal`, `thresholdIsAlert`, `customThresholdIsRespected`, `thresholdValidationAcceptsBounds` y `thresholdValidationRejectsOutOfRange`.

### Comprobación estática reproducible

1. Revisar `PlantDatabase.findLatestByName()`: la consulta usa `LIKE ?` y entrega el patrón por separado a `rawQuery` (`arrayOf("%$filter%")`).
2. Revisar `PlantRepository`: el parámetro `Context` no se conserva como propiedad y la base recibe `context.applicationContext`.
3. Revisar la fábrica del ViewModel en `MainActivity`: el Repository se crea con `applicationContext`.

**Resultado observado:** se confirmaron las tres condiciones en el código modificado.

La suite automatizada actual verifica las reglas de umbral, no ejecuta consultas SQLite ni recrea Activities. No se inició la aplicación ni un emulador, de acuerdo con la restricción de la Actividad 3. Por eso, la búsqueda con caracteres especiales y la recreación de Activity quedan pendientes de validación en dispositivo.

## Conclusión

Las refactorizaciones reducen riesgos de seguridad en la búsqueda y alinean el ciclo de vida del contexto usado por la capa de datos con el ciclo de vida de la aplicación. Los cambios son localizados y conservan las funcionalidades existentes.

package com.esanchon.buscorua.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.TimeTextDefaults.timeFormat
import com.esanchon.buscorua.presentation.datasource.BusRemoteDataSource
import com.esanchon.buscorua.presentation.datasource.CatalogLocalDataSource
import com.esanchon.buscorua.presentation.datasource.SettingsLocalDataSource
import com.esanchon.buscorua.presentation.domain.Catalog
import com.esanchon.buscorua.presentation.domain.Route
import com.esanchon.buscorua.presentation.domain.RouteSettings
import com.esanchon.buscorua.presentation.repository.CatalogRepository
import com.esanchon.buscorua.presentation.theme.BusCoruñaTheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = SettingsLocalDataSource(applicationContext)
        val catalogRepository = CatalogRepository(BusRemoteDataSource(), CatalogLocalDataSource(applicationContext))
        setContent {
            SettingsScreen(settings, catalogRepository)
        }
    }
}

@Composable
fun SettingsScreen(settings: SettingsLocalDataSource, catalogRepository: CatalogRepository) {
    // Sin catálogo (sin red y nunca descargado) no hay nombres ni listado de líneas
    var catalog by remember { mutableStateOf<Catalog?>(null) }
    LaunchedEffect(Unit) { catalog = catalogRepository.getCatalog() }

    BusCoruñaTheme {
        val listState = rememberScalingLazyListState()
        Scaffold(
            timeText = { TimeText(timeSource = TimeTextDefaults.timeSource(timeFormat())) },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
            positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
        ) {
            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                autoCentering = AutoCenteringParams(itemIndex = 0),
                state = listState
            ) {
                item {
                    Text(
                        text = "⚙️ Configuración",
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item { ListHeader { Text("🏢 Al Trabajo") } }
                item { RouteSettingsSection(settings, catalog, Route.WORK) }
                item { ListHeader { Text("🏠 A Casa") } }
                item { RouteSettingsSection(settings, catalog, Route.HOME) }
            }
        }
    }
}

@Composable
private fun RouteSettingsSection(settings: SettingsLocalDataSource, catalog: Catalog?, route: Route) {
    val context = LocalContext.current
    var routeSettings by remember { mutableStateOf(settings.getRouteSettings(route)) }

    fun saveStops(stops: List<Int>) {
        settings.setStops(route, stops.distinct())
        routeSettings = settings.getRouteSettings(route)
    }

    fun saveLines(lines: List<String>) {
        settings.setLines(route, lines)
        routeSettings = settings.getRouteSettings(route)
    }

    // Índice de la parada que se está cambiando; null = añadiendo una nueva
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    val stopPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val stop = result.data?.getIntExtra(StopPadActivity.EXTRA_STOP, 0)?.takeIf { it > 0 }
            ?: return@rememberLauncherForActivityResult
        val stops = routeSettings.stops.toMutableList()
        val index = editingIndex
        if (index != null && index in stops.indices) stops[index] = stop else stops += stop
        saveStops(stops)
    }

    Column {
        SectionLabel("Paradas")
        routeSettings.stops.forEachIndexed { index, stop ->
            StopRow(
                stop = stop,
                stopName = catalog?.stopName(stop),
                canRemove = routeSettings.stops.size > 1,
                onEdit = {
                    editingIndex = index
                    stopPicker.launch(StopPickerActivity.intent(context, routeSettings.stops - stop))
                },
                onRemove = { saveStops(routeSettings.stops.filterIndexed { i, _ -> i != index }) }
            )
        }
        CompactChip(
            onClick = {
                editingIndex = null
                stopPicker.launch(StopPickerActivity.intent(context, routeSettings.stops))
            },
            label = { Text("+ Añadir parada", fontSize = 12.sp) },
            colors = ChipDefaults.secondaryChipColors(),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 2.dp, bottom = 6.dp)
        )

        SectionLabel("Líneas")
        if (catalog == null) {
            Text(
                text = routeSettings.lines.joinToString(", ").ifEmpty { "Todas" } +
                    "\n(sin conexión: no se puede cambiar)",
                fontSize = 11.sp,
                color = MaterialTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LineToggles(catalog, routeSettings, onChange = ::saveLines)
            RouteWarnings(catalog, routeSettings)
        }
    }
}

/**
 * Casillas con las líneas que pasan por las paradas de la ruta.
 * Ninguna marcada = todas las líneas ("Todas").
 */
@Composable
private fun LineToggles(catalog: Catalog, routeSettings: RouteSettings, onChange: (List<String>) -> Unit) {
    val selected = routeSettings.lines.map { it.uppercase() }.toSet()
    val serving = catalog.linesServing(routeSettings.stops)
    val servingNames = serving.map { it.name.uppercase() }.toSet()
    // Las elegidas que ya no pasan por las paradas siguen visibles para poder desmarcarlas
    val leftovers = routeSettings.lines.filter { it.uppercase() !in servingNames }

    LineToggle(
        label = "Todas",
        secondary = "Cualquier línea",
        checked = selected.isEmpty(),
        onCheckedChange = { if (it) onChange(emptyList()) }
    )
    serving.forEach { line ->
        val name = line.name.uppercase()
        LineToggle(
            label = "Línea ${line.name}",
            secondary = line.description,
            checked = name in selected,
            onCheckedChange = { checked ->
                val others = routeSettings.lines.filter { it.uppercase() != name }
                onChange(if (checked) others + line.name else others)
            }
        )
    }
    leftovers.forEach { line ->
        LineToggle(
            label = "Línea $line",
            secondary = "No pasa por estas paradas",
            checked = true,
            onCheckedChange = { onChange(routeSettings.lines - line) }
        )
    }
}

@Composable
private fun LineToggle(label: String, secondary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ToggleChip(
        checked = checked,
        onCheckedChange = onCheckedChange,
        label = { Text(label) },
        secondaryLabel = secondary.takeIf { it.isNotBlank() }?.let {
            { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        toggleControl = {
            Icon(
                imageVector = ToggleChipDefaults.checkboxIcon(checked),
                contentDescription = if (checked) "Marcada" else "Sin marcar"
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    )
}

/** Avisos, p. ej. paradas guardadas que ya no existen en el catálogo. */
@Composable
private fun RouteWarnings(catalog: Catalog, routeSettings: RouteSettings) {
    val warnings = mutableListOf<String>()
    val unknownStops = catalog.unknownStops(routeSettings.stops)
    if (unknownStops.isNotEmpty()) warnings += notFoundMessage("la parada", "las paradas", unknownStops)
    val unknownLines = catalog.unknownLines(routeSettings.lines)
    if (unknownLines.isNotEmpty()) warnings += notFoundMessage("la línea", "las líneas", unknownLines)

    warnings.forEach { warning ->
        Text(
            text = "⚠️ $warning",
            fontSize = 10.sp,
            color = MaterialTheme.colors.error,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = MaterialTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
    )
}

/** Una parada: al tocarla se cambia por otra; ✕ la quita. */
@Composable
private fun StopRow(
    stop: Int,
    stopName: String?,
    canRemove: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Chip(
            onClick = onEdit,
            label = { Text("$stop") },
            secondaryLabel = stopName?.let {
                { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            },
            colors = ChipDefaults.secondaryChipColors(),
            modifier = Modifier.weight(1f)
        )
        if (canRemove) {
            CompactButton(
                onClick = onRemove,
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Text("✕", fontSize = 12.sp)
            }
        }
    }
}

private fun notFoundMessage(singular: String, plural: String, items: List<Any>) =
    if (items.size == 1) "No existe $singular ${items[0]}"
    else "No existen $plural ${items.joinToString(", ")}"

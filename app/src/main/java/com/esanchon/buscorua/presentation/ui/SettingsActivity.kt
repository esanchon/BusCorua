package com.esanchon.buscorua.presentation.ui

import android.Manifest
import android.app.RemoteInput
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.wear.compose.material.*
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.compose.material.TimeTextDefaults.timeFormat
import com.esanchon.buscorua.presentation.complication.RouteComplicationService
import com.esanchon.buscorua.presentation.datasource.BusRemoteDataSource
import com.esanchon.buscorua.presentation.datasource.LocationDataSource
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

    val context = LocalContext.current
    var routes by remember { mutableStateOf(settings.getRoutes()) }

    // Nombre del destino nuevo, escrito o dictado con la entrada de texto del reloj
    val nameInput = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val name = result.data
            ?.let { RemoteInput.getResultsFromIntent(it) }
            ?.getCharSequence(DESTINATION_NAME_KEY)
            ?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return@rememberLauncherForActivityResult
        settings.addRoute(name.take(30))
        routes = settings.getRoutes()
    }

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
                routes.forEach { route ->
                    item(key = "header_${route.id}") {
                        ListHeader { Text(route.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    item(key = route.id) {
                        Column {
                            RouteSettingsSection(settings, catalog, route)
                            if (route.isCustom) {
                                RemoveRouteButton(route) {
                                    settings.removeRoute(route)
                                    routes = settings.getRoutes()
                                    RouteComplicationService.requestUpdate(context)
                                }
                            }
                        }
                    }
                }
                item {
                    Chip(
                        onClick = {
                            try {
                                nameInput.launch(destinationNameIntent())
                            } catch (err: ActivityNotFoundException) {
                                err.printStackTrace()
                            }
                        },
                        label = { Text("+ Añadir destino") },
                        secondaryLabel = { Text("Gimnasio, colegio…") },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
                item { ListHeader { Text("📍 Ubicación") } }
                item { BackgroundLocationSection() }
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

private const val DESTINATION_NAME_KEY = "destination_name"

/** Pantalla del sistema para escribir o dictar el nombre de un destino. */
private fun destinationNameIntent(): Intent {
    val input = RemoteInput.Builder(DESTINATION_NAME_KEY)
        .setLabel("Nombre del destino")
        .build()
    return RemoteInputIntentHelper.createActionRemoteInputIntent().also {
        RemoteInputIntentHelper.putRemoteInputsExtra(it, listOf(input))
    }
}

/** Borra un destino personalizado; pide tocar otra vez para confirmar. */
@Composable
private fun RemoveRouteButton(route: Route, onRemove: () -> Unit) {
    var confirming by remember(route.id) { mutableStateOf(false) }
    CompactChip(
        onClick = { if (confirming) onRemove() else confirming = true },
        label = {
            Text(
                if (confirming) "Toca otra vez para borrar" else "🗑 Borrar destino",
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = if (confirming) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp)
    )
}

/**
 * "Permitir siempre" para que la complicación "Bus Trabajo-Casa" cambie de ruta
 * sin abrir la app. Android no deja concederlo ni quitarlo con un diálogo: se abre
 * la página de permisos de la app.
 */
@Composable
private fun BackgroundLocationSection() {
    val context = LocalContext.current
    val location = remember { LocationDataSource(context) }
    var granted by remember { mutableStateOf(location.hasBackgroundPermission()) }
    var denied by remember { mutableStateOf(false) }

    // Al volver de los ajustes del sistema se refleja lo que se haya cambiado allí
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = location.hasBackgroundPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun onResult(isGranted: Boolean) {
        granted = location.hasBackgroundPermission()
        denied = !isGranted
        // La complicación vuelve a mirar la ubicación con el permiso nuevo
        if (granted) RouteComplicationService.requestUpdate(context)
    }

    val backgroundRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), ::onResult)
    // El permiso en segundo plano solo se puede pedir con el normal ya concedido
    val foregroundRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundRequest.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            onResult(ok)
        }
    }

    fun openAppSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        )
    }

    Column {
        ToggleChip(
            checked = granted,
            onCheckedChange = {
                when {
                    // Quitarlo solo se puede desde los ajustes del sistema
                    granted -> openAppSettings()
                    !location.hasPermission() -> foregroundRequest.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                        backgroundRequest.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    else -> onResult(true)
                }
            },
            label = { Text("Segundo plano", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            secondaryLabel = {
                Text(
                    if (granted) "Siempre" else "Solo al abrir la app",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            toggleControl = {
                Icon(
                    imageVector = ToggleChipDefaults.switchIcon(granted),
                    contentDescription = if (granted) "Activada" else "Desactivada"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        )
        Text(
            text = if (denied && !granted) "Elige \"Permitir siempre\" en los permisos de ubicación"
            else "Para la complicación \"Bus Trabajo-Casa\"",
            fontSize = 10.sp,
            color = if (denied && !granted) MaterialTheme.colors.error else MaterialTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
        if (denied && !granted) {
            CompactChip(
                onClick = ::openAppSettings,
                label = { Text("Abrir permisos", fontSize = 12.sp) },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp)
            )
        }
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

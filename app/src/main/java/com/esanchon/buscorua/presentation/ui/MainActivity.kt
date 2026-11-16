package com.esanchon.buscorua.presentation.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.TimeTextDefaults.timeFormat
import com.esanchon.buscorua.presentation.datasource.LocationDataSource
import com.esanchon.buscorua.presentation.domain.Route
import com.esanchon.buscorua.presentation.domain.RouteSummary
import com.esanchon.buscorua.presentation.presentation.MainPresenter
import com.esanchon.buscorua.presentation.theme.BusCoruñaTheme

class MainActivity : ComponentActivity(), ViewTranslator {

    var presenter: MainPresenter? = null

    // Estado observado por Compose: al cambiarlo solo se recompone lo necesario,
    // sin reconstruir toda la pantalla con setContent.
    private var routesState by mutableStateOf<List<Route>>(emptyList())
    private var timesState by mutableStateOf<Map<Route, RouteSummary>>(emptyMap())

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) presenter?.loadOrder()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = MainPresenter(viewTranslator = this, context = applicationContext)

        // Se pide al crear la pantalla y no en onResume: el diálogo vuelve a
        // llamar a onResume y, si se deniega, se pediría en bucle
        if (savedInstanceState == null && !LocationDataSource(this).hasPermission()) {
            locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        setContent {
            WearApp(
                routes = routesState,
                times = timesState,
                onOpenRoute = { route -> startActivity(RouteDetailActivity.intent(this, route)) },
                onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) }
            )
        }
    }

    // Se recarga al abrir la app, al volver al primer plano y al volver de la configuración
    // (donde se pueden añadir o borrar destinos) o del detalle
    override fun onResume() {
        super.onResume()
        presenter?.load()
    }

    override fun onDestroy() {
        presenter?.onDestroy()
        super.onDestroy()
    }

    override fun showTimes(times: Map<Route, RouteSummary>) {
        timesState = times
    }

    override fun showOrder(routes: List<Route>) {
        routesState = routes
    }
}

@Composable
fun WearApp(
    routes: List<Route>,
    times: Map<Route, RouteSummary>,
    onOpenRoute: (Route) -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    BusCoruñaTheme {
        val listState = rememberScalingLazyListState()
        Scaffold(
            timeText = {
                TimeText(
                    timeSource = TimeTextDefaults.timeSource(timeFormat())
                )
            },
            vignette = {
                Vignette(vignettePosition = VignettePosition.TopAndBottom)
            },
            positionIndicator = {
                PositionIndicator(
                    scalingLazyListState = listState
                )
            }
        ) {
            val contentModifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)

            ScalingLazyColumn(
                modifier = contentModifier,
                autoCentering = AutoCenteringParams(itemIndex = 0),
                state = listState
            ) {
                item {
                    Text(
                        text = "🚌 Bus Coruña",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    )
                }

                // Primero el destino que sale de donde estás: cerca de casa, "Al Trabajo"
                routes.forEachIndexed { index, route ->
                    // Con clave, Compose mueve la tarjeta en vez de rehacerla
                    item(key = route.id) {
                        val summary = times[route] ?: RouteSummary.LOADING
                        RouteCard(
                            title = route.title,
                            stopNumber = summary.stop,
                            stopName = summary.stopName,
                            lineNum = summary.line,
                            timeText = summary.time,
                            onClick = { onOpenRoute(route) },
                            modifier = Modifier.padding(top = if (index > 0) 4.dp else 0.dp)
                        )
                    }
                }

                item {
                    CompactChip(
                        onClick = onOpenSettings,
                        label = { Text("⚙️ Configuración", fontSize = 12.sp) },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RouteCard(
    title: String,
    stopNumber: String?,
    stopName: String?,
    lineNum: String,
    timeText: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        // Menos relleno arriba que el de serie para que el título no quede separado del borde
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Izquierda: título y línea, uno encima de otro
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Chip(
                    onClick = onClick,
                    // Etiqueta roja con texto blanco, como el fondo del logotipo de Tranvías
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .height(32.dp),
                    label = {
                        Text(
                            text = "Bus $lineNum",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            // Derecha: los minutos ocupan la altura del título y de la línea
            ArrivalTime(timeText)
        }

        // "42 - Emilio González López, Manuel Azaña", recortado si no cabe
        val stopLine = when {
            stopNumber.isNullOrBlank() -> null
            stopName.isNullOrBlank() -> stopNumber
            else -> "$stopNumber - $stopName"
        }
        if (stopLine != null) {
            Text(
                text = stopLine,
                fontSize = 10.sp,
                color = MaterialTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

/** Minutos en grande con "min" debajo; los textos ("...", "Error", "Sin buses") a un tamaño que quepa. */
// includeFontPadding es experimental en Compose 1.2 (estable en versiones posteriores)
@OptIn(ExperimentalTextApi::class)
@Composable
private fun ArrivalTime(timeText: String) {
    val minutes = timeText.toIntOrNull()
    if (minutes == null) {
        Text(
            text = timeText,
            fontSize = if (timeText.length <= 3) 32.sp else 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        return
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Text(
            text = "$minutes",
            // Con 3 cifras se reduce para no comerse el título
            fontSize = if (minutes < 100) 52.sp else 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colors.primary,
            maxLines = 1,
            // Sin el margen de fuente que Android añade arriba y abajo: la fila no crece de más
            style = LocalTextStyle.current.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
        Text(
            text = "min",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary,
            // Acerca "min" al número (las fuentes grandes dejan hueco debajo)
            modifier = Modifier.offset(y = (-2).dp)
        )
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    val gym = Route("preview", "Gimnasio")
    WearApp(
        routes = listOf(Route.WORK, Route.HOME, gym),
        times = mapOf(
            Route.WORK to RouteSummary("5", "3", "42", "Emilio González López, Manuel Azaña"),
            Route.HOME to RouteSummary("12", "3A", "3", "Av. da Mariña, Obelisco"),
            gym to RouteSummary("Sin paradas", "-", null, null)
        )
    )
}

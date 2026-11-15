package com.esanchon.buscorua.presentation.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import androidx.wear.compose.material.TimeTextDefaults.timeFormat
import com.esanchon.buscorua.presentation.complication.RouteComplicationService
import com.esanchon.buscorua.presentation.domain.BusArrival
import com.esanchon.buscorua.presentation.domain.Route
import com.esanchon.buscorua.presentation.domain.RouteArrivals
import com.esanchon.buscorua.presentation.repository.BusRepository
import com.esanchon.buscorua.presentation.theme.BusCoruñaTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Lista de todos los próximos buses de una ruta, ordenados por minutos. Tocar uno actualiza. */
class RouteDetailActivity : ComponentActivity() {

    private lateinit var repository: BusRepository
    private lateinit var route: Route
    private var state by mutableStateOf<RouteArrivals?>(null)
    private var loading by mutableStateOf(false)
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        route = intent.getStringExtra(EXTRA_ROUTE)?.let { runCatching { Route.valueOf(it) }.getOrNull() } ?: Route.WORK
        repository = BusRepository.create(applicationContext)

        // Se parte de lo que muestra la tarjeta, para que el primer bus sea el mismo
        state = repository.lastRouteArrivals(route)
            ?.takeIf { System.currentTimeMillis() - it.loadedAt < REUSE_MS }

        setContent {
            RouteDetailScreen(route = route, result = state, loading = loading, onRefresh = ::load)
        }
        if (state == null) load()
    }

    private fun load() {
        loadJob?.cancel()
        loading = true
        loadJob = lifecycleScope.launch {
            state = repository.getRouteArrivals(route)
            loading = false
            RouteComplicationService.requestUpdate(applicationContext)
        }
    }

    companion object {
        private const val EXTRA_ROUTE = "route"
        private const val REUSE_MS = 30_000L

        fun intent(context: Context, route: Route): Intent =
            Intent(context, RouteDetailActivity::class.java).putExtra(EXTRA_ROUTE, route.name)
    }
}

@Composable
fun RouteDetailScreen(route: Route, result: RouteArrivals?, loading: Boolean, onRefresh: () -> Unit) {
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
                        text = if (route == Route.WORK) "🏢 Al Trabajo" else "🏠 A Casa",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val arrivals = result?.arrivals
                when {
                    result == null || (loading && arrivals.isNullOrEmpty()) -> item { StatusText("...") }
                    arrivals == null -> item { StatusCard("Error al consultar", onRefresh) }
                    arrivals.isEmpty() -> item { StatusCard("Sin buses de tus líneas", onRefresh) }
                    else -> {
                        items(arrivals.size) { index ->
                            ArrivalCard(arrivals[index], onClick = onRefresh)
                        }
                        item {
                            Text(
                                text = if (loading) "Actualizando…" else "Toca un bus para actualizar",
                                fontSize = 10.sp,
                                color = MaterialTheme.colors.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// includeFontPadding es experimental en Compose 1.2 (estable en versiones posteriores)
@OptIn(ExperimentalTextApi::class)
@Composable
private fun ArrivalCard(arrival: BusArrival, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bus ${arrival.line}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    text = arrival.stopName?.let { "${arrival.stop} - $it" } ?: "${arrival.stop}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = "${arrival.minutes}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colors.primary,
                    maxLines = 1,
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Text(
                    text = "min",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
            }
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colors.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StatusCard(text: String, onRefresh: () -> Unit) {
    Card(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$text\nToca para reintentar",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun RouteDetailPreview() {
    RouteDetailScreen(
        route = Route.WORK,
        result = RouteArrivals(
            stops = listOf(42, 44),
            arrivals = listOf(
                BusArrival(4, "3A", 42, "Emilio González López, Manuel Azaña"),
                BusArrival(11, "3", 44, "Castelao, Estrada dos Fortes"),
                BusArrival(26, "3A", 42, "Emilio González López, Manuel Azaña")
            ),
            stopName = null
        ),
        loading = false,
        onRefresh = {}
    )
}

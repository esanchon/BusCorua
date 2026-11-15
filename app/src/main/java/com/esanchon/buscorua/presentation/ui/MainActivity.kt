package com.esanchon.buscorua.presentation.ui

import android.content.Intent
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.esanchon.buscorua.presentation.domain.BusTime
import com.esanchon.buscorua.presentation.domain.Route
import com.esanchon.buscorua.presentation.presentation.MainPresenter
import com.esanchon.buscorua.presentation.theme.BusCoruñaTheme

class MainActivity : ComponentActivity(), ViewTranslator {

    var presenter: MainPresenter? = null

    // Estado observado por Compose: al cambiarlo solo se recompone lo necesario,
    // sin reconstruir toda la pantalla con setContent.
    private var busTimeState by mutableStateOf(loadingBusTime())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = MainPresenter(viewTranslator = this, context = applicationContext)
        setContent {
            WearApp(
                busTimeState,
                onOpenRoute = { route -> startActivity(RouteDetailActivity.intent(this, route)) },
                onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) }
            )
        }
    }

    // Se recarga al abrir la app, al volver al primer plano y al volver de la configuración o del detalle
    override fun onResume() {
        super.onResume()
        loadBusTimes()
    }

    override fun onDestroy() {
        presenter?.onDestroy()
        super.onDestroy()
    }

    fun loadBusTimes() {
        busTimeState = loadingBusTime()

        // GPS Disabled: Loads directly with default location without requesting GPS or permissions.
        loadWithDefaultLocation()
    }

    private fun loadWithDefaultLocation() {
        val defaultLocation = Location("").apply {
            latitude = 43.37135
            longitude = -8.396
        }
        presenter?.onCreate(defaultLocation)
    }

    override fun showTime(busTime: BusTime) {
        busTimeState = busTime
    }

    override fun showError() {
        busTimeState = BusTime().apply {
            time = "Error"
            timeToHome = "Error"
            line = "-"
            lineToHome = "-"
        }
    }

    private fun loadingBusTime() = BusTime().apply {
        time = "..."
        timeToHome = "..."
    }
}

@Composable
fun WearApp(busTime: BusTime, onOpenRoute: (Route) -> Unit = {}, onOpenSettings: () -> Unit = {}) {
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

                // Card: Al Trabajo
                item {
                    RouteCard(
                        title = "🏢 Al Trabajo",
                        stopNumber = busTime.stopWork,
                        stopName = busTime.stopWorkName,
                        lineNum = busTime.line ?: "-",
                        timeText = busTime.time ?: "N/A",
                        onClick = { onOpenRoute(Route.WORK) }
                    )
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Card: A Casa
                item {
                    RouteCard(
                        title = "🏠 A Casa",
                        stopNumber = busTime.stopHome,
                        stopName = busTime.stopHomeName,
                        lineNum = busTime.lineToHome ?: "-",
                        timeText = busTime.timeToHome ?: "N/A",
                        onClick = { onOpenRoute(Route.HOME) }
                    )
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
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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
    WearApp(BusTime().apply {
        time = "5"
        line = "3"
        stopWork = "42"
        timeToHome = "12"
        lineToHome = "3A"
        stopHome = "3"
        stopWorkName = "Emilio González López, Manuel Azaña"
        stopHomeName = "Av. da Mariña, Obelisco"
    })
}

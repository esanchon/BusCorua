package com.esanchon.buscorua.presentation.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.TimeTextDefaults.timeFormat
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import com.esanchon.buscorua.presentation.complication.RouteComplicationService
import com.esanchon.buscorua.presentation.datasource.SettingsLocalDataSource
import com.esanchon.buscorua.presentation.domain.Route
import com.esanchon.buscorua.presentation.theme.BusCoruñaTheme

/**
 * Se abre al poner la complicación "Bus a destino" en la esfera (o al editarla):
 * elige qué destino muestra ese hueco. Si se sale sin elegir, la esfera no la añade.
 */
class DestinationPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val complicationId = intent.getIntExtra(ComplicationDataSourceService.EXTRA_CONFIG_COMPLICATION_ID, -1)
        val settings = SettingsLocalDataSource(applicationContext)
        setResult(Activity.RESULT_CANCELED)

        setContent {
            DestinationPickerScreen(
                routes = settings.getRoutes(),
                current = settings.getComplicationRoute(complicationId),
                onPick = { route ->
                    if (complicationId != -1) settings.setComplicationRoute(complicationId, route)
                    setResult(Activity.RESULT_OK)
                    RouteComplicationService.requestUpdate(applicationContext)
                    finish()
                }
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun DestinationPickerScreen(routes: List<Route>, current: Route?, onPick: (Route) -> Unit) {
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
                        text = "Bus a destino",
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                routes.forEach { route ->
                    item(key = route.id) {
                        Chip(
                            onClick = { onPick(route) },
                            label = { Text(route.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            secondaryLabel = if (route == current) {
                                { Text("Elegido") }
                            } else null,
                            colors = if (route == current) ChipDefaults.primaryChipColors()
                            else ChipDefaults.secondaryChipColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        )
                    }
                }
                if (routes.none { it.isCustom }) {
                    item {
                        Text(
                            text = "Añade más destinos en la app: ⚙️ Configuración → + Añadir destino",
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

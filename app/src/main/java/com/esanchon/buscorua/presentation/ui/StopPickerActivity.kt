package com.esanchon.buscorua.presentation.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.esanchon.buscorua.presentation.datasource.BusRemoteDataSource
import com.esanchon.buscorua.presentation.datasource.CatalogLocalDataSource
import com.esanchon.buscorua.presentation.domain.Catalog
import com.esanchon.buscorua.presentation.repository.CatalogRepository
import com.esanchon.buscorua.presentation.theme.BusCoruñaTheme

/**
 * Selector de parada en dos pasos:
 *  - sin [EXTRA_LINE]: lista de líneas (y opción de teclear el número);
 *  - con [EXTRA_LINE]: paradas de esa línea en orden de recorrido.
 * Devuelve la parada elegida en [StopPadActivity.EXTRA_STOP].
 */
class StopPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lineId = intent.getStringExtra(EXTRA_LINE)
        val selected = intent.getIntArrayExtra(EXTRA_SELECTED)?.toSet().orEmpty()
        val catalogRepository = CatalogRepository(BusRemoteDataSource(), CatalogLocalDataSource(applicationContext))

        setContent {
            var catalog by remember { mutableStateOf<Catalog?>(null) }
            var loading by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                catalog = catalogRepository.getCatalog()
                loading = false
            }

            // Lo que elijan las pantallas hijas (paradas de una línea o teclado) se devuelve tal cual
            val child = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    setResult(Activity.RESULT_OK, result.data)
                    finish()
                }
            }

            StopPickerScreen(
                catalog = catalog,
                loading = loading,
                lineId = lineId,
                selected = selected,
                onPickLine = { child.launch(StopPickerActivity.intent(this, selected, it)) },
                onTypeNumber = { child.launch(StopPadActivity.intent(this)) },
                onPickStop = { stop ->
                    setResult(Activity.RESULT_OK, Intent().putExtra(StopPadActivity.EXTRA_STOP, stop))
                    finish()
                }
            )
        }
    }

    companion object {
        private const val EXTRA_LINE = "line"
        private const val EXTRA_SELECTED = "selected"

        /** @param selected paradas que ya tiene la ruta, se marcan y no se pueden volver a elegir. */
        fun intent(context: Context, selected: Collection<Int>, lineId: String? = null): Intent =
            Intent(context, StopPickerActivity::class.java)
                .putExtra(EXTRA_SELECTED, selected.toIntArray())
                .apply { if (lineId != null) putExtra(EXTRA_LINE, lineId) }
    }
}

@Composable
private fun StopPickerScreen(
    catalog: Catalog?,
    loading: Boolean,
    lineId: String?,
    selected: Set<Int>,
    onPickLine: (String) -> Unit,
    onTypeNumber: () -> Unit,
    onPickStop: (Int) -> Unit
) {
    BusCoruñaTheme {
        val listState = rememberScalingLazyListState()
        Scaffold(
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
                val line = lineId?.let { catalog?.lines?.get(it) }
                item {
                    ListHeader {
                        Text(if (line != null) "Línea ${line.name}" else "Añadir parada", textAlign = TextAlign.Center)
                    }
                }

                when {
                    catalog == null -> {
                        if (lineId == null) item { TypeNumberChip(onTypeNumber) }
                        item {
                            Text(
                                text = if (loading) "Cargando…" else "Sin conexión: no se pudo cargar el listado",
                                fontSize = 11.sp,
                                color = MaterialTheme.colors.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    line != null -> {
                        items(line.stops.size) { index ->
                            val stop = line.stops[index]
                            val alreadyAdded = stop in selected
                            Chip(
                                onClick = { onPickStop(stop) },
                                enabled = !alreadyAdded,
                                label = { Text(if (alreadyAdded) "$stop ✓" else "$stop") },
                                secondaryLabel = catalog.stopName(stop)?.let {
                                    { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                                },
                                colors = ChipDefaults.secondaryChipColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    else -> {
                        item { TypeNumberChip(onTypeNumber) }
                        item {
                            Text(
                                text = "O elige la línea:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colors.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                        val lines = catalog.sortedLines
                        items(lines.size) { index ->
                            val info = lines[index]
                            Chip(
                                onClick = { onPickLine(info.id) },
                                label = { Text("Línea ${info.name}") },
                                secondaryLabel = info.description.takeIf { it.isNotBlank() }?.let {
                                    { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                },
                                colors = ChipDefaults.secondaryChipColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeNumberChip(onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        label = { Text("🔢 Escribir número") },
        colors = ChipDefaults.primaryChipColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

package com.esanchon.buscorua.presentation.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.esanchon.buscorua.presentation.datasource.BusRemoteDataSource
import com.esanchon.buscorua.presentation.datasource.CatalogLocalDataSource
import com.esanchon.buscorua.presentation.domain.Catalog
import com.esanchon.buscorua.presentation.repository.CatalogRepository
import com.esanchon.buscorua.presentation.theme.BusCoruñaTheme

/** Teclado numérico para introducir un número de parada. Devuelve [EXTRA_STOP]. */
class StopPadActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initial = intent.getIntExtra(EXTRA_STOP, 0).takeIf { it > 0 }?.toString().orEmpty()
        val catalogRepository = CatalogRepository(BusRemoteDataSource(), CatalogLocalDataSource(applicationContext))

        setContent {
            var catalog by remember { mutableStateOf<Catalog?>(null) }
            LaunchedEffect(Unit) { catalog = catalogRepository.getCatalog() }

            StopPad(initial = initial, catalog = catalog) { stop ->
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_STOP, stop))
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_STOP = "stop"

        fun intent(context: Context, stop: Int? = null): Intent =
            Intent(context, StopPadActivity::class.java).apply {
                if (stop != null) putExtra(EXTRA_STOP, stop)
            }
    }
}

private const val MAX_DIGITS = 5
private val KEY_SIZE = 34.dp
private val KEY_SPACING = 4.dp

@Composable
fun StopPad(initial: String, catalog: Catalog?, onConfirm: (Int) -> Unit) {
    var digits by remember { mutableStateOf(initial) }
    val stop = digits.toIntOrNull()
    // Sin catálogo no se puede comprobar: se acepta cualquier número
    val stopName = stop?.let { catalog?.stopName(it) }
    val exists = stop != null && (catalog == null || stopName != null)

    BusCoruñaTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digits.ifEmpty { "Nº parada" },
                fontSize = if (digits.isEmpty()) 14.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (digits.isEmpty()) MaterialTheme.colors.onSurfaceVariant else MaterialTheme.colors.primary
            )
            Text(
                text = when {
                    stop == null -> " "
                    catalog == null -> " "
                    stopName == null -> "No existe"
                    else -> stopName
                },
                fontSize = 10.sp,
                color = if (stop != null && catalog != null && stopName == null) MaterialTheme.colors.error
                else MaterialTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(130.dp)
                    .padding(bottom = 4.dp)
            )

            val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
            rows.forEach { row ->
                KeyRow {
                    row.forEach { key ->
                        Key(key) { if (digits.length < MAX_DIGITS) digits = (digits + key).trimStart('0') }
                    }
                }
            }
            KeyRow {
                Key("⌫", enabled = digits.isNotEmpty(), background = MaterialTheme.colors.surface) {
                    digits = digits.dropLast(1)
                }
                Key("0", enabled = digits.isNotEmpty()) {
                    if (digits.length < MAX_DIGITS) digits += "0"
                }
                Key(
                    "✓",
                    enabled = exists,
                    background = MaterialTheme.colors.primary,
                    contentColor = MaterialTheme.colors.onPrimary
                ) {
                    stop?.let(onConfirm)
                }
            }
        }
    }
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(KEY_SPACING),
        modifier = Modifier.padding(vertical = KEY_SPACING / 2),
        content = content
    )
}

@Composable
private fun Key(
    label: String,
    enabled: Boolean = true,
    background: Color = MaterialTheme.colors.surface,
    contentColor: Color = MaterialTheme.colors.onSurface,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(KEY_SIZE)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = contentColor)
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun StopPadPreview() {
    StopPad(initial = "42", catalog = null) {}
}

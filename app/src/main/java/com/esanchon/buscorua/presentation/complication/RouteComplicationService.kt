package com.esanchon.buscorua.presentation.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.CountDownTimeReference
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.esanchon.buscorua.R
import com.esanchon.buscorua.presentation.datasource.SettingsLocalDataSource
import com.esanchon.buscorua.presentation.domain.BusArrival
import com.esanchon.buscorua.presentation.domain.GetRouteOrderUseCase
import com.esanchon.buscorua.presentation.domain.Route
import com.esanchon.buscorua.presentation.repository.BusRepository
import com.esanchon.buscorua.presentation.ui.RouteDetailActivity
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Complicación de la esfera con el próximo bus de una ruta.
 * Los minutos se envían como hora de llegada: la esfera hace la cuenta atrás sola,
 * así el número no se queda congelado entre actualizaciones (Wear OS permite como mínimo cada 5 min).
 */
abstract class RouteComplicationService : SuspendingComplicationDataSourceService() {

    /** Ruta que muestra esta complicación en esta actualización; null si no tiene ninguna. */
    protected abstract suspend fun route(request: ComplicationRequest): Route?

    /** Ruta de la vista previa del selector de complicaciones. */
    protected open val previewRoute: Route = Route.WORK

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val route = route(request)
            ?: return build(request.complicationType, null, null, null, status = "Elige")
        val repository = BusRepository.create(applicationContext)
        // Si la app acaba de consultar (p. ej. al abrirla) se reutiliza ese resultado
        val result = repository.lastRouteArrivals(route)
            ?.takeIf { System.currentTimeMillis() - it.loadedAt < REUSE_MS }
            ?: repository.getRouteArrivals(route)

        val next = result.arrivals?.firstOrNull()
        val status = when {
            result.stops.isEmpty() -> "Sin parada"
            result.arrivals == null -> "Error"
            next == null -> "Sin bus"
            else -> null
        }
        val arrivalAt = next?.let { Instant.ofEpochMilli(result.loadedAt + it.minutes * 60_000L) }
        return build(request.complicationType, route, next, arrivalAt, status)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        build(
            type,
            previewRoute,
            BusArrival(minutes = 4, line = "3A", stop = 42, stopName = "Emilio González López"),
            Instant.now().plusSeconds(4 * 60),
            status = null
        )

    private fun build(
        type: ComplicationType,
        route: Route?,
        next: BusArrival?,
        arrivalAt: Instant?,
        status: String?
    ): ComplicationData? {
        val text: ComplicationText = if (next != null && arrivalAt != null) {
            // Círculo: "7m 3A" (el icono indica la ruta). Alargada: "Bus 3A en 7 min"
            countdown(arrivalAt, if (type == ComplicationType.LONG_TEXT) "Bus ${next.line} en ^1" else "^1 ${next.line}")
        } else {
            PlainComplicationText.Builder(status ?: "--").build()
        }
        // Etiqueta en la esfera alargada: "aT", "aC" o el nombre del destino
        val shortLabel = when (route) {
            null -> "Bus a destino"
            Route.WORK -> "aT"
            Route.HOME -> "aC"
            else -> route.name
        }
        val iconRes = when (route) {
            Route.WORK -> R.drawable.ic_complication_work
            Route.HOME -> R.drawable.ic_complication_home
            else -> R.drawable.ic_complication_place
        }
        val description = PlainComplicationText.Builder(
            when {
                route == null -> "Bus a destino: mantén pulsada la esfera para elegir el destino"
                next != null -> "Bus ${route.label}: línea ${next.line} en ${next.minutes} minutos, parada ${next.stop}"
                else -> "Bus ${route.label}: ${status ?: "sin datos"}"
            }
        ).build()
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, iconRes)).build()
        val tapAction = route?.let(::openDetail)

        return when (type) {
            // Sin título: las esferas de Samsung, si hay título, pintan título + texto y omiten el icono
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(text, description)
                .setMonochromaticImage(icon)
                .setTapAction(tapAction)
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(text, description)
                .setTitle(
                    PlainComplicationText.Builder(
                        // "aT · 42 - Emilio González López"
                        listOfNotNull(
                            shortLabel,
                            next?.let { a -> a.stopName?.let { "${a.stop} - $it" } ?: "${a.stop}" }
                        ).joinToString(" · ")
                    ).build()
                )
                .setMonochromaticImage(icon)
                .setTapAction(tapAction)
                .build()

            else -> NoDataComplicationData()
        }
    }

    /** "4 min" que la esfera va descontando hasta mostrar "ahora". [template] usa ^1 para el tiempo. */
    private fun countdown(arrivalAt: Instant, template: String?): ComplicationText =
        TimeDifferenceComplicationText.Builder(
            TimeDifferenceStyle.SHORT_SINGLE_UNIT,
            CountDownTimeReference(arrivalAt)
        )
            .setMinimumTimeUnit(TimeUnit.MINUTES)
            .setDisplayAsNow(true)
            .apply { if (template != null) setText(template) }
            .build()

    /** Tocar el círculo abre la lista de próximos buses de la ruta. */
    private fun openDetail(route: Route): PendingIntent =
        PendingIntent.getActivity(
            this,
            route.id.hashCode(),
            RouteDetailActivity.intent(this, route),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    companion object {
        private const val REUSE_MS = 60_000L

        /** Pide a la esfera que vuelva a leer las complicaciones (tras consultar o cambiar la configuración). */
        fun requestUpdate(context: Context) {
            listOf(
                WorkComplicationService::class.java,
                HomeComplicationService::class.java,
                NearestComplicationService::class.java,
                DestinationComplicationService::class.java
            ).forEach {
                ComplicationDataSourceUpdateRequester
                    .create(context, ComponentName(context, it))
                    .requestUpdateAll()
            }
        }
    }
}

class WorkComplicationService : RouteComplicationService() {
    override suspend fun route(request: ComplicationRequest) = Route.WORK
}

class HomeComplicationService : RouteComplicationService() {
    override suspend fun route(request: ComplicationRequest) = Route.HOME
    override val previewRoute = Route.HOME
}

/**
 * "Bus Trabajo-Casa": siempre el bus al trabajo o a casa, el que sale de donde estás
 * (cerca de casa, al trabajo con el maletín; cerca del trabajo, a casa con la casa).
 * Los destinos personalizados no cuentan: para ellos está "Bus a destino".
 * Sin ubicación se usa el último orden.
 */
class NearestComplicationService : RouteComplicationService() {
    override suspend fun route(request: ComplicationRequest) =
        GetRouteOrderUseCase.create(applicationContext).execute().firstWorkOrHome()
}

/** Trabajo o casa, el primero de los dos en [this] (orden por cercanía). */
fun List<Route>.firstWorkOrHome(): Route = firstOrNull { !it.isCustom } ?: Route.WORK

/**
 * "Bus a destino": al ponerla en la esfera se elige el destino ([DestinationPickerActivity]),
 * así se puede tener una por cada destino personalizado. Se guarda por hueco de la esfera.
 */
class DestinationComplicationService : RouteComplicationService() {
    override val previewRoute = Route("preview", "Destino")

    override suspend fun route(request: ComplicationRequest) =
        SettingsLocalDataSource(applicationContext).getComplicationRoute(request.complicationInstanceId)

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        SettingsLocalDataSource(applicationContext).removeComplicationRoute(complicationInstanceId)
    }
}

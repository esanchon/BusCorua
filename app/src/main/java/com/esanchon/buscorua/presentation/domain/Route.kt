package com.esanchon.buscorua.presentation.domain

/**
 * Un destino: "Al trabajo", "A casa" o uno que añade el usuario ("Gimnasio").
 * [id] es la clave con la que se guarda su configuración; dos rutas son la misma si tienen el mismo id.
 */
class Route(val id: String, val name: String) {

    val isCustom: Boolean get() = id != WORK_ID && id != HOME_ID

    /** Título de tarjetas y pantallas: "🏢 Al Trabajo", "📍 Gimnasio". */
    val title: String
        get() = when (id) {
            WORK_ID -> "🏢 Al Trabajo"
            HOME_ID -> "🏠 A Casa"
            else -> "📍 $name"
        }

    /** Para frases: "Bus al trabajo", "Bus a Gimnasio". */
    val label: String
        get() = when (id) {
            WORK_ID -> "al trabajo"
            HOME_ID -> "a casa"
            else -> "a $name"
        }

    override fun equals(other: Any?) = other is Route && other.id == id
    override fun hashCode() = id.hashCode()
    override fun toString() = "Route($id, $name)"

    companion object {
        private const val WORK_ID = "work"
        private const val HOME_ID = "home"

        val WORK = Route(WORK_ID, "Trabajo")
        val HOME = Route(HOME_ID, "Casa")
    }
}

/**
 * Configuración de una ruta.
 * @param stops paradas donde se puede coger el bus. Vacía solo en un destino recién creado.
 * @param lines nombres comerciales de las líneas que sirven ("3", "3A"...). Vacía = todas.
 */
data class RouteSettings(val stops: List<Int>, val lines: List<String>)

/** Un bus que llegará a [stop] en [minutes] minutos. [lineId] es el código interno de la API. */
data class Arrival(val stop: Int, val lineId: String, val minutes: Int)

/** Un bus que cumple la configuración de la ruta, listo para mostrar. */
data class BusArrival(val minutes: Int, val line: String, val stop: Int, val stopName: String?)

/**
 * Próximos buses de una ruta ordenados por minutos.
 * @param arrivals null si fallaron todas las peticiones.
 * @param stopName nombre de la parada cuando la ruta tiene solo una (para mostrarlo aunque no haya buses).
 */
data class RouteArrivals(
    val stops: List<Int>,
    val arrivals: List<BusArrival>?,
    val stopName: String?,
    val loadedAt: Long = System.currentTimeMillis()
)

/** Lo que muestra la tarjeta de una ruta en la pantalla principal. */
data class RouteSummary(
    val time: String,
    val line: String,
    val stop: String?,
    val stopName: String?
) {
    companion object {
        val LOADING = RouteSummary(time = "...", line = "-", stop = null, stopName = null)

        fun from(result: RouteArrivals): RouteSummary {
            val next = result.arrivals?.firstOrNull()
            return RouteSummary(
                time = when {
                    result.stops.isEmpty() -> "Sin paradas"
                    result.arrivals == null -> "Error"
                    next == null -> "Sin buses"
                    else -> next.minutes.toString()
                },
                line = next?.line ?: "-",
                // Sin buses, todas las paradas de la ruta ("42, 44")
                stop = next?.stop?.toString() ?: result.stops.joinToString(", ").ifEmpty { null },
                stopName = next?.stopName ?: result.stopName
            )
        }
    }
}

package com.esanchon.buscorua.presentation.domain

enum class Route { WORK, HOME }

/**
 * Configuración de una ruta.
 * @param stops paradas donde se puede coger el bus (al menos una).
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

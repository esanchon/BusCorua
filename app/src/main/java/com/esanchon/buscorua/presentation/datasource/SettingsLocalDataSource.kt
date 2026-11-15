package com.esanchon.buscorua.presentation.datasource

import android.content.Context
import com.esanchon.buscorua.presentation.domain.Route
import com.esanchon.buscorua.presentation.domain.RouteSettings

/** Guarda la configuración del usuario. */
class SettingsLocalDataSource(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun getRouteSettings(route: Route): RouteSettings {
        val stops = prefs.getString(stopsKey(route), null)
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.ifEmpty { null }
            ?: defaultStops(route)
        // Sin guardar -> valores por defecto; guardado vacío -> todas las líneas
        val lines = prefs.getString(linesKey(route), null)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: DEFAULT_LINES
        return RouteSettings(stops, lines)
    }

    fun setStops(route: Route, stops: List<Int>) {
        prefs.edit().putString(stopsKey(route), stops.joinToString(",")).apply()
    }

    fun setLines(route: Route, lines: List<String>) {
        prefs.edit().putString(linesKey(route), lines.joinToString(",")).apply()
    }

    private fun stopsKey(route: Route) = "stops_${route.name.lowercase()}"
    private fun linesKey(route: Route) = "lines_${route.name.lowercase()}"

    private fun defaultStops(route: Route) = when (route) {
        Route.WORK -> listOf(42, 44)
        Route.HOME -> listOf(159, 3)
    }

    companion object {
        private val DEFAULT_LINES = listOf("3", "3A")
    }
}

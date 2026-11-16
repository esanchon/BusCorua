package com.esanchon.buscorua.presentation.datasource

import android.content.Context
import com.esanchon.buscorua.presentation.domain.Route
import com.esanchon.buscorua.presentation.domain.RouteSettings

/** Guarda la configuración del usuario. */
class SettingsLocalDataSource(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    // ---- Destinos ----

    /** Al trabajo, a casa y los destinos personalizados, en el orden en que se crearon. */
    fun getRoutes(): List<Route> =
        listOf(Route.WORK, Route.HOME) + customIds().map { Route(it, prefs.getString(nameKey(it), null) ?: "Destino") }

    fun getRoute(id: String): Route? = getRoutes().firstOrNull { it.id == id }

    /** Crea un destino personalizado sin paradas (se añaden después en la configuración). */
    fun addRoute(name: String): Route {
        val id = "c" + System.currentTimeMillis().toString(36)
        prefs.edit()
            .putString(CUSTOM_ROUTES_KEY, (customIds() + id).joinToString(","))
            .putString(nameKey(id), name)
            .apply()
        return Route(id, name)
    }

    /** Borra un destino personalizado y su configuración. Casa y trabajo no se pueden borrar. */
    fun removeRoute(route: Route) {
        if (!route.isCustom) return
        prefs.edit()
            .putString(CUSTOM_ROUTES_KEY, (customIds() - route.id).joinToString(","))
            .remove(nameKey(route.id))
            .remove(stopsKey(route))
            .remove(linesKey(route))
            .apply()
    }

    private fun customIds(): List<String> =
        prefs.getString(CUSTOM_ROUTES_KEY, null)?.split(",")?.filter { it.isNotBlank() }.orEmpty()

    // ---- Paradas y líneas de cada destino ----

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
            ?: defaultLines(route)
        return RouteSettings(stops, lines)
    }

    fun setStops(route: Route, stops: List<Int>) {
        prefs.edit().putString(stopsKey(route), stops.joinToString(",")).apply()
    }

    fun setLines(route: Route, lines: List<String>) {
        prefs.edit().putString(linesKey(route), lines.joinToString(",")).apply()
    }

    private fun stopsKey(route: Route) = "stops_${route.id}"
    private fun linesKey(route: Route) = "lines_${route.id}"
    private fun nameKey(id: String) = "name_$id"

    private fun defaultStops(route: Route) = when (route) {
        Route.WORK -> listOf(42, 44)
        Route.HOME -> listOf(159, 3)
        else -> emptyList()
    }

    private fun defaultLines(route: Route) = if (route.isCustom) emptyList() else DEFAULT_LINES

    // ---- Orden de las tarjetas ----

    /**
     * Destinos en el orden de la última vez (el más cercano primero), para abrir la app
     * igual. Los que no estaban entonces van al final.
     */
    fun getRouteOrder(): List<Route> {
        val routes = getRoutes()
        val saved = prefs.getString(ROUTE_ORDER_KEY, null)?.split(",")
            // Versión anterior: solo se guardaba cuál iba primero ("WORK" / "HOME")
            ?: prefs.getString(LEGACY_FIRST_ROUTE_KEY, null)?.let { listOf(it.lowercase()) }
            ?: emptyList()
        return routes.sortedBy { route -> saved.indexOf(route.id).takeIf { it >= 0 } ?: Int.MAX_VALUE }
    }

    fun setRouteOrder(routes: List<Route>) {
        prefs.edit().putString(ROUTE_ORDER_KEY, routes.joinToString(",") { it.id }).apply()
    }

    // ---- Complicación "Bus a destino" ----

    /** Destino elegido para una complicación concreta de la esfera; null si no tiene o se borró. */
    fun getComplicationRoute(complicationId: Int): Route? =
        prefs.getString(complicationKey(complicationId), null)?.let(::getRoute)

    fun setComplicationRoute(complicationId: Int, route: Route) {
        prefs.edit().putString(complicationKey(complicationId), route.id).apply()
    }

    fun removeComplicationRoute(complicationId: Int) {
        prefs.edit().remove(complicationKey(complicationId)).apply()
    }

    private fun complicationKey(id: Int) = "complication_$id"

    companion object {
        private val DEFAULT_LINES = listOf("3", "3A")
        private const val CUSTOM_ROUTES_KEY = "custom_routes"
        private const val ROUTE_ORDER_KEY = "route_order"
        private const val LEGACY_FIRST_ROUTE_KEY = "first_route"
    }
}

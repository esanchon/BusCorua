package com.esanchon.buscorua.presentation.repository

import com.esanchon.buscorua.presentation.domain.Route
import com.esanchon.buscorua.presentation.domain.RouteArrivals

interface BusRepositoryContract {
    /** Próximos buses de varias rutas, consultadas todas en paralelo. */
    suspend fun getAllRouteArrivals(routes: List<Route>): Map<Route, RouteArrivals>

    /** Todos los próximos buses de la ruta que cumplen su configuración, ordenados por minutos. */
    suspend fun getRouteArrivals(route: Route): RouteArrivals

    /** Última consulta hecha de esa ruta (la que se ve en la tarjeta), o null. */
    fun lastRouteArrivals(route: Route): RouteArrivals?
}

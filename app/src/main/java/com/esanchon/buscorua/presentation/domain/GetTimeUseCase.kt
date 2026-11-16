package com.esanchon.buscorua.presentation.domain

import com.esanchon.buscorua.presentation.repository.BusRepositoryContract

class GetTimeUseCase(var busRepositoryContract: BusRepositoryContract) {

    /** Próximos buses de todos los destinos, consultados en paralelo. */
    suspend fun execute(routes: List<Route>): Map<Route, RouteArrivals> =
        busRepositoryContract.getAllRouteArrivals(routes)
}

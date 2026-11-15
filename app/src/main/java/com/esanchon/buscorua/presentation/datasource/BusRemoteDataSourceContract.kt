package com.esanchon.buscorua.presentation.datasource

import com.esanchon.buscorua.presentation.domain.Arrival

interface BusRemoteDataSourceContract {
    /** Próximos buses de una parada. null si la petición falla. */
    fun getArrivals(busStop: Int): List<Arrival>?

    /** Catálogo completo (líneas y paradas) como JSON. null si falla. */
    fun getCatalogJson(): String?
}

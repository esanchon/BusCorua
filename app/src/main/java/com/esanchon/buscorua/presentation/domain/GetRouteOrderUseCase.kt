package com.esanchon.buscorua.presentation.domain

import android.content.Context
import android.location.Location
import com.esanchon.buscorua.presentation.datasource.BusRemoteDataSource
import com.esanchon.buscorua.presentation.datasource.CatalogLocalDataSource
import com.esanchon.buscorua.presentation.datasource.LocationDataSource
import com.esanchon.buscorua.presentation.datasource.SettingsLocalDataSource
import com.esanchon.buscorua.presentation.repository.CatalogRepository

/**
 * Ordena los destinos según dónde estás. Las paradas de un destino son donde coges
 * el bus para ir allí: las de "Al trabajo" están cerca de casa y las de "A casa" cerca
 * del trabajo. Primero va el destino cuya parada más cercana tienes más cerca.
 * Si dos empatan (mismas paradas), se respeta el orden de creación.
 * Sin ubicación, se mantiene el último orden.
 */
class GetRouteOrderUseCase(
    private val settings: SettingsLocalDataSource,
    private val catalogRepository: CatalogRepository,
    private val locationDataSource: LocationDataSource
) {

    /** Orden guardado, para pintar la pantalla antes de tener la ubicación. */
    fun lastOrder(): List<Route> = settings.getRouteOrder()

    suspend fun execute(): List<Route> {
        val location = locationDataSource.getLocation() ?: return settings.getRouteOrder()
        val catalog = catalogRepository.getCatalog() ?: return settings.getRouteOrder()

        // Los destinos sin paradas con posición, al final
        val ordered = settings.getRoutes().sortedBy { nearestStopDistance(location, catalog, it) ?: Float.MAX_VALUE }
        settings.setRouteOrder(ordered)
        return ordered
    }

    /** Metros hasta la parada más cercana de la ruta, null si ninguna tiene posición. */
    private fun nearestStopDistance(location: Location, catalog: Catalog, route: Route): Float? =
        settings.getRouteSettings(route).stops
            .mapNotNull { catalog.stops[it] }
            .mapNotNull { stop ->
                val lat = stop.latitude ?: return@mapNotNull null
                val lon = stop.longitude ?: return@mapNotNull null
                val result = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, lat, lon, result)
                result[0]
            }
            .minOrNull()

    companion object {
        fun create(context: Context): GetRouteOrderUseCase {
            val appContext = context.applicationContext
            return GetRouteOrderUseCase(
                SettingsLocalDataSource(appContext),
                CatalogRepository(BusRemoteDataSource(), CatalogLocalDataSource(appContext)),
                LocationDataSource(appContext)
            )
        }
    }
}

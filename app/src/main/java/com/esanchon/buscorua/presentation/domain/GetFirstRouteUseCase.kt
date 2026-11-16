package com.esanchon.buscorua.presentation.domain

import android.content.Context
import android.location.Location
import com.esanchon.buscorua.presentation.datasource.BusRemoteDataSource
import com.esanchon.buscorua.presentation.datasource.CatalogLocalDataSource
import com.esanchon.buscorua.presentation.datasource.LocationDataSource
import com.esanchon.buscorua.presentation.datasource.SettingsLocalDataSource
import com.esanchon.buscorua.presentation.repository.CatalogRepository

/**
 * Decide qué tarjeta va primero según dónde estás. Las paradas de "Al trabajo" están
 * cerca de casa y las de "A casa" cerca del trabajo, así que va primero la ruta
 * cuya parada más cercana tienes más cerca. Sin ubicación, se mantiene el último orden.
 */
class GetFirstRouteUseCase(
    private val settings: SettingsLocalDataSource,
    private val catalogRepository: CatalogRepository,
    private val locationDataSource: LocationDataSource
) {

    /** Orden guardado, para pintar la pantalla antes de tener la ubicación. */
    fun lastFirstRoute(): Route = settings.getFirstRoute()

    suspend fun execute(): Route {
        val location = locationDataSource.getLocation() ?: return settings.getFirstRoute()
        val catalog = catalogRepository.getCatalog() ?: return settings.getFirstRoute()

        val toWork = nearestStopDistance(location, catalog, Route.WORK)
        val toHome = nearestStopDistance(location, catalog, Route.HOME)
        val first = when {
            toWork == null || toHome == null -> return settings.getFirstRoute()
            toWork <= toHome -> Route.WORK
            else -> Route.HOME
        }
        settings.setFirstRoute(first)
        return first
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
        fun create(context: Context): GetFirstRouteUseCase {
            val appContext = context.applicationContext
            return GetFirstRouteUseCase(
                SettingsLocalDataSource(appContext),
                CatalogRepository(BusRemoteDataSource(), CatalogLocalDataSource(appContext)),
                LocationDataSource(appContext)
            )
        }
    }
}

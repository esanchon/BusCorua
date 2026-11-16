package com.esanchon.buscorua.presentation.repository

import android.content.Context
import com.esanchon.buscorua.presentation.datasource.BusRemoteDataSource
import com.esanchon.buscorua.presentation.datasource.BusRemoteDataSourceContract
import com.esanchon.buscorua.presentation.datasource.CatalogLocalDataSource
import com.esanchon.buscorua.presentation.datasource.SettingsLocalDataSource
import com.esanchon.buscorua.presentation.domain.Arrival
import com.esanchon.buscorua.presentation.domain.BusArrival
import com.esanchon.buscorua.presentation.domain.Catalog
import com.esanchon.buscorua.presentation.domain.Route
import com.esanchon.buscorua.presentation.domain.RouteArrivals
import com.esanchon.buscorua.presentation.domain.RouteSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

class BusRepository(
    var remoteDataSourceContract: BusRemoteDataSourceContract,
    var settingsDataSource: SettingsLocalDataSource,
    var catalogRepository: CatalogRepository
) : BusRepositoryContract {

    override suspend fun getAllRouteArrivals(routes: List<Route>): Map<Route, RouteArrivals> = coroutineScope {
        // Todas las rutas (y todas sus paradas) se piden en paralelo
        routes.map { route -> async { route to getRouteArrivals(route) } }.awaitAll().toMap()
    }

    override suspend fun getRouteArrivals(route: Route): RouteArrivals = coroutineScope {
        val settings = settingsDataSource.getRouteSettings(route)
        // Destino recién creado, aún sin paradas: nada que consultar
        if (settings.stops.isEmpty()) {
            return@coroutineScope RouteArrivals(stops = emptyList(), arrivals = emptyList(), stopName = null)
                .also { lastResults[route.id] = it }
        }
        val catalogRequest = async { catalogRepository.getCatalog() }
        val arrivals = fetchArrivals(settings.stops)
        val catalog = catalogRequest.await()

        buildRouteArrivals(settings, arrivals, catalog).also { lastResults[route.id] = it }
    }

    override fun lastRouteArrivals(route: Route): RouteArrivals? = lastResults[route.id]

    /** Llegadas de todas las paradas juntas. null solo si fallan todas las peticiones. */
    private suspend fun fetchArrivals(stops: List<Int>): List<Arrival>? = coroutineScope {
        val results = stops
            .map { stop -> async(Dispatchers.IO) { remoteDataSourceContract.getArrivals(stop) } }
            .awaitAll()
        if (results.all { it == null }) null else results.filterNotNull().flatten()
    }

    private fun buildRouteArrivals(
        settings: RouteSettings,
        arrivals: List<Arrival>?,
        catalog: Catalog?
    ): RouteArrivals {
        fun lineName(lineId: String) = catalog?.lineName(lineId) ?: lineId

        val wanted = settings.lines.map { it.uppercase() }.toSet()
        val filtered = arrivals
            ?.filter { wanted.isEmpty() || lineName(it.lineId).uppercase() in wanted || it.lineId in wanted }
            ?.sortedBy { it.minutes }
            ?.map { BusArrival(it.minutes, lineName(it.lineId), it.stop, catalog?.stopName(it.stop)) }

        return RouteArrivals(
            stops = settings.stops,
            arrivals = filtered,
            stopName = settings.stops.singleOrNull()?.let { catalog?.stopName(it) }
        )
    }

    companion object {
        // Compartido entre pantallas: la lista de detalle parte de lo que muestra la tarjeta
        private val lastResults = ConcurrentHashMap<String, RouteArrivals>()

        fun create(context: Context): BusRepository {
            val appContext = context.applicationContext
            val remote = BusRemoteDataSource()
            return BusRepository(
                remote,
                SettingsLocalDataSource(appContext),
                CatalogRepository(remote, CatalogLocalDataSource(appContext))
            )
        }
    }
}

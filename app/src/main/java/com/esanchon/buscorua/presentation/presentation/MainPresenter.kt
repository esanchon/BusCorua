package com.esanchon.buscorua.presentation.presentation

import android.content.Context
import android.location.Location
import com.esanchon.buscorua.presentation.complication.RouteComplicationService
import com.esanchon.buscorua.presentation.domain.GetFirstRouteUseCase
import com.esanchon.buscorua.presentation.domain.GetTimeUseCase
import com.esanchon.buscorua.presentation.domain.Route
import com.esanchon.buscorua.presentation.repository.BusRepository
import com.esanchon.buscorua.presentation.ui.ViewTranslator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainPresenter(var viewTranslator: ViewTranslator, context: Context) {

    private val appContext = context.applicationContext

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val getTimeUseCase = GetTimeUseCase(BusRepository.create(context))
    private var loadJob: Job? = null

    private val getFirstRouteUseCase = GetFirstRouteUseCase.create(appContext)
    private var orderJob: Job? = null

    /** Orden de la última vez, para pintar sin esperar a la ubicación. */
    fun initialFirstRoute(): Route = getFirstRouteUseCase.lastFirstRoute()

    /** Busca la ubicación (puede tardar unos segundos) y recoloca las tarjetas. */
    fun loadFirstRoute() {
        orderJob?.cancel()
        orderJob = scope.launch {
            val previous = getFirstRouteUseCase.lastFirstRoute()
            val first = getFirstRouteUseCase.execute()
            viewTranslator.showFirstRoute(first)
            // Si has cambiado de sitio, la complicación "según ubicación" cambia ya de icono
            if (first != previous) RouteComplicationService.requestUpdate(appContext)
        }
    }

    fun onCreate(location: Location) {
        // Si se pulsa varias veces seguidas, solo cuenta la última carga
        loadJob?.cancel()
        loadJob = scope.launch {
            val busTime = getTimeUseCase.execute(location)
            if (busTime != null) {
                viewTranslator.showTime(busTime)
                // Los círculos de la esfera se ponen al día con lo que se acaba de consultar
                RouteComplicationService.requestUpdate(appContext)
            } else {
                viewTranslator.showError()
            }
        }
    }

    fun onDestroy() {
        scope.cancel()
    }
}

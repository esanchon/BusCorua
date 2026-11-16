package com.esanchon.buscorua.presentation.presentation

import android.content.Context
import com.esanchon.buscorua.presentation.complication.RouteComplicationService
import com.esanchon.buscorua.presentation.complication.firstWorkOrHome
import com.esanchon.buscorua.presentation.domain.GetRouteOrderUseCase
import com.esanchon.buscorua.presentation.domain.GetTimeUseCase
import com.esanchon.buscorua.presentation.domain.RouteSummary
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
    private val getRouteOrderUseCase = GetRouteOrderUseCase.create(appContext)
    private var loadJob: Job? = null
    private var orderJob: Job? = null

    /**
     * Pinta los destinos en el orden de la última vez, consulta sus buses y,
     * en paralelo, busca la ubicación para recolocarlos.
     */
    fun load() {
        val routes = getRouteOrderUseCase.lastOrder()
        viewTranslator.showOrder(routes)
        viewTranslator.showTimes(routes.associateWith { RouteSummary.LOADING })

        // Si se pulsa varias veces seguidas, solo cuenta la última carga
        loadJob?.cancel()
        loadJob = scope.launch {
            val results = getTimeUseCase.execute(routes)
            viewTranslator.showTimes(results.mapValues { RouteSummary.from(it.value) })
            // Los círculos de la esfera se ponen al día con lo que se acaba de consultar
            RouteComplicationService.requestUpdate(appContext)
        }

        loadOrder()
    }

    /** Busca la ubicación (puede tardar unos segundos) y recoloca las tarjetas. Sin permiso no cambia nada. */
    fun loadOrder() {
        orderJob?.cancel()
        orderJob = scope.launch {
            val previous = getRouteOrderUseCase.lastOrder().firstWorkOrHome()
            val order = getRouteOrderUseCase.execute()
            viewTranslator.showOrder(order)
            // Si has cambiado de sitio, la complicación "Bus Trabajo-Casa" cambia ya de icono
            if (order.firstWorkOrHome() != previous) RouteComplicationService.requestUpdate(appContext)
        }
    }

    fun onDestroy() {
        scope.cancel()
    }
}

package com.esanchon.buscorua.presentation.ui

import com.esanchon.buscorua.presentation.domain.Route
import com.esanchon.buscorua.presentation.domain.RouteSummary

interface ViewTranslator {

    /** Próximo bus de cada destino. */
    fun showTimes(times: Map<Route, RouteSummary>)

    /** Destinos en el orden de las tarjetas: primero el que sale de donde estás. */
    fun showOrder(routes: List<Route>)
}

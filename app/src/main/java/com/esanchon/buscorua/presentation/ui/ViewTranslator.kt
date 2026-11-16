package com.esanchon.buscorua.presentation.ui

import com.esanchon.buscorua.presentation.domain.BusTime
import com.esanchon.buscorua.presentation.domain.Route

interface ViewTranslator {

    fun showTime(busTime: BusTime)

    fun showError()

    /** Ruta cuya tarjeta va arriba, según lo cerca que estés de sus paradas. */
    fun showFirstRoute(route: Route)
}

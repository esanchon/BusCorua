package com.esanchon.buscorua.presentation.ui

import com.esanchon.buscorua.presentation.domain.BusTime

interface ViewTranslator {

    fun showTime(busTime: BusTime)
    fun showError()
}
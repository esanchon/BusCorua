package com.esanchon.buscorua.presentation.repository

import com.esanchon.buscorua.presentation.domain.BusTime

interface BusRepositoryContract {
    fun getTime() : BusTime?

}
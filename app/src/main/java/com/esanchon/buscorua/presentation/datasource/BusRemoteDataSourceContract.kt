package com.esanchon.buscorua.presentation.datasource

import com.esanchon.buscorua.presentation.domain.BusTime

interface BusRemoteDataSourceContract {
    fun getTime() : BusTime?

}
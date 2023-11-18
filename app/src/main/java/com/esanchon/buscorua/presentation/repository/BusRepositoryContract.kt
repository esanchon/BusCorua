package com.esanchon.buscorua.presentation.repository

import android.location.Location
import com.esanchon.buscorua.presentation.domain.BusTime

interface BusRepositoryContract {
    fun getBusTime(location: Location) : BusTime?

}
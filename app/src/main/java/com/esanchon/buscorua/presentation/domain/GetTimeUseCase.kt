package com.esanchon.buscorua.presentation.domain

import android.location.Location
import com.esanchon.buscorua.presentation.repository.BusRepositoryContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetTimeUseCase(var busRepositoryContract: BusRepositoryContract) {

    suspend fun execute(location: Location): BusTime? = withContext(Dispatchers.IO) {
        busRepositoryContract.getBusTime(location)
    }
}

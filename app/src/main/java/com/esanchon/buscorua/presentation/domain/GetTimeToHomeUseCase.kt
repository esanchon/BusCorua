package com.esanchon.buscorua.presentation.domain

import com.esanchon.buscorua.presentation.repository.BusRepositoryContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetTimeToHomeUseCase(var busRepositoryContract: BusRepositoryContract) {

    suspend fun execute(): BusTime? = withContext(Dispatchers.Default)  {
        busRepositoryContract.getTimeToHome()
    }
}

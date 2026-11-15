package com.esanchon.buscorua.presentation.presentation

import android.location.Location
import com.esanchon.buscorua.presentation.datasource.BusRemoteDataSource
import com.esanchon.buscorua.presentation.datasource.LocationLocalDataSource
import com.esanchon.buscorua.presentation.domain.GetTimeUseCase
import com.esanchon.buscorua.presentation.repository.BusRepository
import com.esanchon.buscorua.presentation.ui.ViewTranslator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainPresenter(var viewTranslator: ViewTranslator) {

    fun onCreate(location: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            val getTimeUseCase = GetTimeUseCase(BusRepository(BusRemoteDataSource(), LocationLocalDataSource()))
            val busTime = getTimeUseCase.execute(location)

            withContext(Dispatchers.Main) {
                if (busTime != null) {
                    viewTranslator.showTime(busTime)
                } else {
                    viewTranslator.showError()
                }
            }
        }
    }
}
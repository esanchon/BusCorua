package com.esanchon.buscorua.presentation.presentation

import com.esanchon.buscorua.presentation.datasource.BusRemoteDataSource
import com.esanchon.buscorua.presentation.domain.GetTimeToHomeUseCase
import com.esanchon.buscorua.presentation.domain.GetTimeToWorkUseCase
import com.esanchon.buscorua.presentation.repository.BusRepository
import com.esanchon.buscorua.presentation.ui.ViewTranslator
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainPresenter(var viewTranslator: ViewTranslator) {

    fun onCreate() {
        runBlocking(){
            val getTimeToWorkUseCase = GetTimeToWorkUseCase(BusRepository(BusRemoteDataSource()))
            val getTimeToHomeUseCase = GetTimeToHomeUseCase(BusRepository(BusRemoteDataSource()))


            GlobalScope.launch {
                val busTime =  getTimeToWorkUseCase.execute()

                busTime?.let {
                    viewTranslator.showTime(it)
                } ?: run {
                    viewTranslator.showError()}
            }

        }
    }
}
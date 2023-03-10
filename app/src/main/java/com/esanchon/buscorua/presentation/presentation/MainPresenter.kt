package com.esanchon.buscorua.presentation.presentation

import com.esanchon.buscorua.presentation.datasource.BusRemoteDataSource
import com.esanchon.buscorua.presentation.domain.GetTimeUseCase
import com.esanchon.buscorua.presentation.repository.BusRepository
import com.esanchon.buscorua.presentation.ui.MainActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainPresenter(var viewTranslator: MainActivity) {

    fun onCreate() {
        runBlocking(){
            val getTimeUseCase = GetTimeUseCase(BusRepository(BusRemoteDataSource()))


            GlobalScope.launch {
                val busTime =  getTimeUseCase.execute()

                busTime?.let {
                    viewTranslator.showTime(it)
                } ?: run {
                    viewTranslator.showError()}


            }

        }
    }


}
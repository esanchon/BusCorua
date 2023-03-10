package com.esanchon.buscorua.presentation.repository

import com.esanchon.buscorua.presentation.datasource.BusRemoteDataSourceContract
import com.esanchon.buscorua.presentation.domain.BusTime

class BusRepository(var remoteDataSourceContract: BusRemoteDataSourceContract) : BusRepositoryContract{

    override fun getTime() : BusTime? {
        return remoteDataSourceContract.getTime()
    }


}
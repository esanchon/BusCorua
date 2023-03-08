package com.esanchon.buscorua.presentation.repository

import com.esanchon.buscorua.presentation.datasource.BusRemoteDataSourceContract
import com.esanchon.buscorua.presentation.domain.BusTime

class BusRepository : BusRepositoryContract{

    var remoteDataSourceContract : BusRemoteDataSourceContract

    constructor(remoteDataSourceContract : BusRemoteDataSourceContract) {
        this.remoteDataSourceContract = remoteDataSourceContract
    }

    override fun getTimeToWork() : BusTime? {
        return remoteDataSourceContract.getTimeToWork()
    }

    override fun getTimeToHome() : BusTime? {
        return remoteDataSourceContract.getTimeToHome()
    }
}
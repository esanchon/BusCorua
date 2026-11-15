package com.esanchon.buscorua.presentation.repository

import android.location.Location
import com.esanchon.buscorua.presentation.datasource.BusRemoteDataSourceContract
import com.esanchon.buscorua.presentation.datasource.LocationLocalDataSourceContract
import com.esanchon.buscorua.presentation.domain.BusTime

class BusRepository(var remoteDataSourceContract: BusRemoteDataSourceContract,
                    var localPositionDatasource: LocationLocalDataSourceContract)
    : BusRepositoryContract{

    override fun getBusTime(location: Location) : BusTime? {
        val workBusTime = remoteDataSourceContract.getTime(42)
        val homeBusTime = remoteDataSourceContract.getTime(3)

        val result = BusTime()
        result.time = workBusTime?.time ?: "N/A"
        result.line = workBusTime?.line ?: "301"
        result.timeToHome = homeBusTime?.time ?: "N/A"
        result.lineToHome = homeBusTime?.line ?: "3"
        return result
    }
}
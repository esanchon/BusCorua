package com.esanchon.buscorua.presentation.repository

import android.location.Location
import com.esanchon.buscorua.presentation.datasource.BusRemoteDataSourceContract
import com.esanchon.buscorua.presentation.datasource.LocationLocalDataSourceContract
import com.esanchon.buscorua.presentation.domain.BusTime

class BusRepository(var remoteDataSourceContract: BusRemoteDataSourceContract,
                    var localPositionDatasource: LocationLocalDataSourceContract)
    : BusRepositoryContract{

    override fun getBusTime(location: Location) : BusTime? {
        val busStop = localPositionDatasource.getClosestStop(location)
       return  if(busStop == 0){
            return BusTime()
        } else {

            remoteDataSourceContract.getTime(busStop)
        }

    }


}
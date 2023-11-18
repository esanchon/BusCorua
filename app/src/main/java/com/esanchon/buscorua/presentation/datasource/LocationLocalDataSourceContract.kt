package com.esanchon.buscorua.presentation.datasource

import android.location.Location


interface LocationLocalDataSourceContract {
    fun getClosestStop(location:Location) : Int

}
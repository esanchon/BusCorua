package com.esanchon.buscorua.presentation.datasource

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.esanchon.buscorua.presentation.BusApplication
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener


class LocationLocalDataSource : LocationLocalDataSourceContract {

    val home = Location("").apply {
        latitude = 43.374237
        longitude= -8.431628
    }
    val work = Location("").apply{
        latitude = 43.368644
        longitude = -8.402186
    }
    var current = Location("")?.apply {
        latitude = 0.0
        longitude = 0.0
    }


    override fun getClosestStop(location: Location): Int {

        var closestStop = 42

        return compareLocations(location,work,home)

//        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(BusApplication.appContext!!)
//
//        if (ActivityCompat.checkSelfPermission(
//                BusApplication.appContext!!,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                BusApplication.appContext!!,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            return closestStop
//        } else {
//
//            fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, object : CancellationToken() {
//                override fun onCanceledRequested(p0: OnTokenCanceledListener) = CancellationTokenSource().token
//
//                override fun isCancellationRequested() = false
//            })
//                .addOnSuccessListener { location: Location? ->
//                    if (location == null)
//
//                    else {
//                        current = location
//                        closestStop = compareLocations(current!!,home,work)
//
//
//                    }
//
//                }
//
//        }

        return closestStop

    }

    private fun compareLocations(current: Location, home: Location, work: Location):Int {
        return if(current!!.distanceTo(home) < current!!.distanceTo(work)){
            42
        }else{
            3
        }
    }
}

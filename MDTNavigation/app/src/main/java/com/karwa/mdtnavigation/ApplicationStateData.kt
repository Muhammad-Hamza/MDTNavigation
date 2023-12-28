package com.karwa.mdtnavigation

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.app.ActivityCompat

/**
 * Created by mHamza on 12/20/2023.
 */
 class ApplicationStateData : Application() {



    lateinit var location: Location
    private val inProgressRouteType = 1
    private val currentTripId = ""
    private var locationObserver: KLocationObserver? = null
    private  val DEFAULT_LAT = 25.193747
    private  val DEFAULT_LON = 51.474661
    private var etaToStop = -1.0

    companion object {
        @Volatile
        private var INSTANCE: ApplicationStateData? = null

        fun getInstance() =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApplicationStateData().also { INSTANCE = it }
            }
    }
    init {
        INSTANCE = this
    }

    override fun onCreate() {
        super.onCreate()
        initBackgroundServices()
    }

    fun setCurrentLocation(loc: Location?) {
        if (loc != null) {
//            LOGD(TAG, "Location: Setting: " + loc.getLatitude() +", "+loc.getLongitude());
            location = loc
        }
    }
    private fun initBackgroundServices() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        startServices()
    }

    fun startServices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, BackgroundLocationService::class.java))
        }
    }

    fun getCurrentLocation(): Location {
        return if (!::location.isInitialized) {
            val loc = Location("")
            loc.latitude = DEFAULT_LAT
            loc.longitude = DEFAULT_LON
            loc
        } else location
    }
    fun getLocationObserver(): KLocationObserver? {
        return locationObserver
    }

    fun registerLocationObserver(locationObserver: KLocationObserver?) {
        this.locationObserver = locationObserver
    }


    fun getEtaToStop(): Double {
        return etaToStop
    }

    fun setEtaToStop(etaToStop: Double) {
        this.etaToStop = etaToStop
    }


    lateinit var txtArrivalTime: String
    lateinit var txtRemainingTime: String
    lateinit var txtRemainingDistance: String
    var arrivalTime: Long = 0

}
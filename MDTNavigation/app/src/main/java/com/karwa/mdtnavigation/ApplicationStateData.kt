package com.karwa.mdtnavigation

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.app.ActivityCompat

/**
 * Created by Oashraf on 11/20/2019.
 */
object ApplicationStateData : Application() {
    lateinit var location: Location
    private val inProgressRouteType = 1
    private val currentTripId = ""
    private var locationObserver: KLocationObserver? = null
    private const val DEFAULT_LAT = 25.193747
    private const val DEFAULT_LON = 51.474661
    private var etaToStop = -1.0

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
        //initAITP();
//        MQTTManager.getInstance();
        //      SignalRManager.getInstance();
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        startServices()

//        if (StoredConfig.getInstance().scannerEnable)
//            startSerialPortListening();
    }

    fun startServices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, BackgroundLocationService::class.java))
        }
    }

    fun getCurrentLocation(): Location {
        return if (location == null) {
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
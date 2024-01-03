package com.karwa.mdtnavigation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.karwa.mdtnavigation.databinding.MainActivityBinding
import com.mapbox.geojson.Point
import java.util.Timer
import java.util.TimerTask


class MainActivity : AppCompatActivity() {
    lateinit var binding:MainActivityBinding
    var mapApplication: MapApplication? = null
    val MY_PERMISSIONS_REQUEST_READ_LOCATION = 7
    private lateinit var requestLocationPermission: ActivityResultLauncher<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.main_activity)
        //initMap()

        requestLocationPermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    startNavigation()
                }else{
                    Toast.makeText(this,"Please allow permission from app settings",Toast.LENGTH_SHORT).show()
                }
            }


        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {


            startNavigation()
        } else {
            requestLocationPermission()
        }



    }

    private fun startNavigation(){
        initMap()
        startServices()
        Handler().postDelayed(Runnable {
            runOnUiThread{
                val intent = intent
                if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
                    val route: String = intent.data?.getQueryParameter("ROUTE_INTENT").toString()
                    mapApplication?.clearNavigation()
//                    mapApplication?.clearRoute()
                    mapApplication?.startNavigation(Point.fromLngLat(ApplicationStateData.getInstance().getCurrentLocation().longitude, ApplicationStateData.getInstance().getCurrentLocation().latitude),route)
                    startTimer()
//                    Toast.makeText(applicationContext, route,Toast.LENGTH_SHORT).show()
                }
            }

        },1000)
    }


    private fun startServices() {
        startForegroundService(Intent(this, BackgroundLocationService::class.java))
    }
    private fun requestLocationPermission() {
        requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    fun initMap() {
        mapApplication = MapApplication(binding.navigationView, binding.maneuverView)
    }


    override fun onResume() {
        super.onResume()
        if (mapApplication != null) {
            mapApplication!!.stopNavigation()
            mapApplication!!.registerLocationObserver()
                if (mapApplication != null) {
                    mapApplication!!.onStart()
               }
        }
    }

    private fun updateBottomBar(isResetting: Boolean) {
        runOnUiThread {
            binding.bottomBarLayout.distanceToDestination.text =
                if (isResetting) "--" else ApplicationStateData.getInstance().txtRemainingDistance
            binding.bottomBarLayout.arrivalTimeText.text =
                if (isResetting) "--" else ApplicationStateData.getInstance().txtArrivalTime
            if (isResetting) binding.bottomBarLayout.etaFirstPieceVal.text = "--" else {
                val etaParts: List<String> =
                    ApplicationStateData.getInstance().txtRemainingTime.split("<br>")
                if (etaParts != null && etaParts.size > 0) {
                    val etaFirstPart =
                        etaParts[0].split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    if (etaFirstPart != null && etaFirstPart.size == 2) {
                        binding.bottomBarLayout.etaFirstPieceVal.text = etaFirstPart[0]
                        binding.bottomBarLayout.etaFirstPieceUnit.text = etaFirstPart[1]
                    }
                    if (etaParts.size == 2) {
                        val etaSecondtPart =
                            etaParts[1].split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        if (etaSecondtPart != null && etaSecondtPart.size == 2) {
                            binding.bottomBarLayout.etaSecondPieceVal.text = etaSecondtPart[0]
                            binding.bottomBarLayout.etaSecondPieceUnit.text = etaSecondtPart[1]
                        }
                    } else binding.bottomBarLayout.etaSecondsContainer.visibility = View.GONE
                } else {
                    binding.bottomBarLayout.etaFirstPieceVal.text = "--"
                    binding.bottomBarLayout.etaFirstPieceUnit.text = "--"
                }
            }
        }
    }
    private val onTripTimerHandler = Handler()

    private fun startTimer(){
       var onTripTimer = Timer()
        val onTripTimerTask: TimerTask = object : TimerTask() {
            override fun run() {
                onTripTimerHandler.post(Runnable {
                    updateBottomBar(false)
                })
            }
        }
        onTripTimer.scheduleAtFixedRate(onTripTimerTask, 0, 1000)
    }


    override fun onStop() {
        super.onStop()
        mapApplication?.onStop()
    }
}

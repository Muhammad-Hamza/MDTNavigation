package com.karwa.mdtnavigation

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.databinding.DataBindingUtil
import com.karwa.mdtnavigation.databinding.MainActivityBinding
import com.mapbox.geojson.Point
import java.util.Timer
import java.util.TimerTask


class MainActivity : ComponentActivity() {
    lateinit var binding:MainActivityBinding
    var mapApplication: MapApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Handler().postDelayed(Runnable {
            runOnUiThread{
                binding = DataBindingUtil.setContentView(this, R.layout.main_activity)

                initMap()
                val intent = intent
                if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
                    val route: String = intent.data?.getQueryParameter("ROUTE_INTENT").toString()
                    mapApplication?.startNavigation(Point.fromLngLat(ApplicationStateData.getCurrentLocation().longitude, ApplicationStateData.getCurrentLocation().latitude),route)
                    startTimer()
                    Toast.makeText(applicationContext, route,Toast.LENGTH_SHORT).show()
                }
            }

        },1000)


    }
    fun initMap() {
        mapApplication =
            MapApplication(binding.navigationView, binding.maneuverView)
    }


    private fun updateBottomBar(isResetting: Boolean) {
        runOnUiThread {
            binding.bottomBarLayout.distanceToDestination.text =
                if (isResetting) "--" else ApplicationStateData.txtRemainingDistance
            binding.bottomBarLayout.arrivalTimeText.text =
                if (isResetting) "--" else ApplicationStateData.txtArrivalTime
            if (isResetting) binding.bottomBarLayout.etaFirstPieceVal.text = "--" else {
                val etaParts: List<String> =
                    ApplicationStateData.txtRemainingTime.split("<br>")
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
    }
}

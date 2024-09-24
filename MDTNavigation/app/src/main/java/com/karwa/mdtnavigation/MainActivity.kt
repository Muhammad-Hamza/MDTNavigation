package com.karwa.mdtnavigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.karwa.mdtnavigation.databinding.MainActivityBinding
import com.karwa.mdtnavigation.log.FirebaseLogger
import com.mapbox.geojson.Point
import java.util.Timer
import java.util.TimerTask
import kotlin.math.log


class MainActivity : AppCompatActivity() {
    lateinit var binding: MainActivityBinding
    var mapApplication: MapApplication? = null
    lateinit var looger: FirebaseLogger

    //    var mapApplication: CustomMapApplication? = null
    val MY_PERMISSIONS_REQUEST_READ_LOCATION = 7
    private lateinit var requestLocationPermission: ActivityResultLauncher<String>
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    private fun getCurrentLocation(onLocationReceived: (Location) -> Unit) {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            looger.logSelectContent(
                "Location Permission",
                "Permission Denied",
                "Permission not granted to access location"
            )
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                looger.logSelectContent(
                    "Location Update",
                    "Success",
                    "Successfully retrieved current location"
                )
                onLocationReceived(location)
            } else {
                looger.logSelectContent("Location Update", "Failure", "Current location is null")
                Log.e("MainActivity", "Current location is null.")
            }
        }.addOnFailureListener { exception ->
            looger.logSelectContent(
                "Location Update",
                "Error",
                "Error getting current location: ${exception.message}"
            )
            Log.e("MainActivity", "Error getting current location: ${exception.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.main_activity)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        //initMap()
        looger = FirebaseLogger.getInstance(this)
        requestLocationPermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    startNavigation()
                } else {
                    Toast.makeText(
                        this,
                        "Please allow permission from app settings",
                        Toast.LENGTH_SHORT
                    ).show()
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


        binding.btnOffRoute.setOnClickListener({
            looger.logSelectContent("Button Click", "Off Route", "Off Route button clicked")
            mapApplication?.calculateOffRouting()
        })

        binding.btnDrawNextLayout.setOnClickListener {
            looger.logSelectContent("Button Click", "Draw Next", "Draw Next Layout button clicked")
            binding.btnDrawNextLayout.visibility = View.GONE
            mapApplication!!.startNextRoute(true)
        }
    }

    private fun startNavigation() {
        initMap()
        startServices()
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            runOnUiThread {
//                val intent = intent
//                var route ="ewwxCuvdyHO_@I]CIAM??BKAKEIGGE?G?GBEBCFAD?D??EHEDi@l@KPaFrC_@LWDWFC?OCIC??iBcGOk@a@qAwAwEEQGQGQEOoAcEeGkRaAcDyFqQ_C}HkEeN]gAuA}EGU??CYAU@QDQFMFMHMJMLKJExJcErB}@bOkG??rA_At@_@nEmBHE??DCDGBGBMIU]gAeCuH_B{EMa@[}@K_@u@wB??"
//                if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
//                    val route: String = intent.data?.getQueryParameter("ROUTE_INTENT").toString()
//                    Route 1
                    val route ="ewwxCuvdyHO_@I]CIAM??BKAKEIGGE?G?GBEBCFAD?D??EHEDi@l@KPaFrC_@LWDWFC?OCIC??iBcGOk@a@qAwAwEEQGQGQEOoAcEeGkRaAcDyFqQ_C}HkEeN]gAuA}EGU??CYAU@QDQFMFMHMJMLKJExJcErB}@bOkG??rA_At@_@nEmBHE??DCDGBGBMIU]gAeCuH_B{EMa@[}@K_@u@wB??"

//                    Second Stop
//                    val route =  "}oxxCqsiyHoA{DmAuDi@_BIWK[CMe@uAoFuP??h@UfDwAf@S??K[Qi@c@sAK[??"

//                    Third Pick
//                    val route =  "izxxCibkyHCGUu@??b@SlAi@dC_AhCkATKj@W~Aq@|BeAlAg@b@S??Zx@Rh@??d@U??\\hADDH@DAdCcABG?I]gA]iA??`@Sf@U??K]??"

//                    Forth Pick
//                    val route =  "{uwxCodjyH|@a@??Pd@??jAi@d@S??Xz@Rh@^lARl@??FRn@YlCgATK??FAHDHFFF@HfAzC??TKNG??FRVp@BDDBF?DAdAc@DE@E?ICIAC??"

                    //Six Pick
//                    val route = "yywxC{ukyHJ\\??g@T??fA|C?B??@HAJALGJWLkCfAo@T??HRp@YhCgARIp@YZOhAe@NELAH@HBFF??dC`IdBvFTr@DJL^HXPb@`G`Q??CLEJEFKHSJaA`@UJm@V]LiCdAm@V??Sm@_@mASi@Y{@??e@RkAh@??Qe@??}@`@??"

                    //7th pick
//                    val route ="kjwxC}jiyHk@aB??p@[FChAe@H@J@HAJA??iBsF??KFWJoAh@aChA}Ah@o@XSHe@V_Br@cC`A??Nd@??~@_@x@]??"

                    //8th pick
//                    val route = "g}vxCu|iyH@BBH?HADEDeAb@E@G?ECCEWq@GS??OFUJ??rBhG??MHWH??_@gACCCAAAE?C@E@{B`AC@AD?BAB@B?D\\`A??QHWFK@I@KAIAiAd@GBq@Z??j@`B??"
                    //Destination
//                    val route = "izwxCmmiyHy@\\_A^??Oe@??bCaA~As@d@W??Vv@jCbIXz@HT??MDoBv@cLzE_Bp@YNKDwAj@sB|@sAj@wItDo@VqMlF??oA`AwErBSHEBCBCFCDADAD??HVd@|A~Qfl@tJl[??RZvB|GDN@FBBDDDBBBF@??NGj@UbIoDr@]nHcDFCfAg@|JiELG~EwBdAc@??Tv@nDtMtA`FPn@Nh@J\\hAfE???TATCNIXIV_EhCw@f@s@b@MHaAn@c@Vi@\\UNaAn@??"

                    looger.logSelectContent("Intent", "Route", "Route intent received: $route")
                    Log.e("asd", route)
                    mapApplication?.stopListenerThing()
//                    mapApplication?.clearRoute()
                    getCurrentLocation({ location ->
                        looger.logSelectContent(
                            "Navigation",
                            "Start with Route",
                            "Starting navigation with route: $route"
                        )
                        mapApplication?.startNavigation(
                            Point.fromLngLat(
                                location.longitude,
                                location.latitude
                            ), route, this
                        )
                        startTimer()
                    })
//                } else {
//                    looger.logSelectContent("Navigation", "No Route", "No route found in intent")
//                    finish()
                }
//            }

        }, 1000)
    }

    private fun startServices() {
        startForegroundService(Intent(this, BackgroundLocationService::class.java))
    }

    private fun requestLocationPermission() {
        requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun initMap() {
        looger.logSelectContent("Map", "Init", "Initializing map")
        mapApplication =
            MapApplication(
                binding.navigationView,
                binding.maneuverView,
                binding.btnOffRoute,
                binding.btnDrawNextLayout,
                looger,
                {
                    Handler(Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 1000)
                })
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        looger.logSelectContent("MainActivity", "onLowMemory", "Low memory warning")
        if (binding != null && binding.navigationView != null)
            binding.navigationView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (binding != null && binding.navigationView != null)
            binding.navigationView.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        if (binding != null && binding.navigationView != null)
            binding.navigationView.onStart()
    }


    override fun onResume() {
        super.onResume()

        if (mapApplication != null) {
            mapApplication?.onResume()
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

    private fun startTimer() {
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
        binding.navigationView.onStop()
        mapApplication?.onStop()
    }
}

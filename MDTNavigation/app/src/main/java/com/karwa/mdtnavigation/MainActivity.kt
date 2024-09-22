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
import com.mapbox.geojson.Point
import java.util.Timer
import java.util.TimerTask


class MainActivity : AppCompatActivity() {
    lateinit var binding: MainActivityBinding
    var mapApplication: MapApplication? = null

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
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                onLocationReceived(location)
            } else {
                // Handle location null (e.g., request single location update)
                Log.e("MainActivity", "Current location is null.")
            }
        }.addOnFailureListener { exception ->
            Log.e("MainActivity", "Error getting current location: ${exception.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.main_activity)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        //initMap()

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
            mapApplication?.calculateOffRouting()
        })

        binding.btnDrawNextLayout.setOnClickListener {
            binding.btnDrawNextLayout.visibility=View.GONE
            mapApplication!!.startNextRoute(true)
        }
    }

    private fun startNavigation() {
        initMap()
        startServices()
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            runOnUiThread {
//                val intent = intent
                var route = ""
//                if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
//                    val route: String = intent.data?.getQueryParameter("ROUTE_INTENT").toString()
//                    val route = "ewwxCuvdyHO_@I]CIAM??BKAKEIGGE?G?GBEBCFAD?D??EHEDi@l@KPaFrC_@LWDWFC?OCIC??iBcGOk@a@qAwAwEEQGQGQEOoAcEeGkRaAcDyFqQ_C}HkEeN]gAuA}EGU??CYAU@QDQFMFMHMJMLKJExJcErB}@bOkG??rA_At@_@nEmBHE??DCDGBGBMIU]gAeCuH_B{EMa@[}@K_@u@wB??"
//                    val route = "}oxxCqsiyHoA{DmAuDi@_BIWK[CMe@uAoFuP??h@UfDwAf@S??K[Qi@c@sAK[??"
//                    val route ="izxxCibkyHCGUu@??b@SlAi@dC_AhCkATKj@W~Aq@|BeAlAg@b@S??Zx@Rh@??d@U??\\hADDH@DAdCcABG?I]gA]iA??`@Sf@U??K]??"
//                    val route ="{uwxCodjyH|@a@??Pd@??jAi@d@S??Xz@Rh@^lARl@??FRn@YlCgATK??FAHDHFFF@HfAzC??TKNG??FRVp@BDDBF?DAdAc@DE@E?ICIAC??"
//                    val route ="yywxC{ukyHJ\\??g@T??fA|C?B??@HAJALGJWLkCfAo@T??HRp@YhCgARIp@YZOhAe@NELAH@HBFF??dC`IdBvFTr@DJL^HXPb@`G`Q??CLEJEFKHSJaA`@UJm@V]LiCdAm@V??Sm@_@mASi@Y{@??e@RkAh@??Qe@??}@`@??"
//                    val route = "kjwxC}jiyHk@aB??p@[FChAe@H@J@HAJA??iBsF??KFWJoAh@aChA}Ah@o@XSHe@V_Br@cC`A??Nd@??~@_@x@]??"
//                    val route = "g}vxCu|iyH@BBH?HADEDeAb@E@G?ECCEWq@GS??OFUJ??rBhG??MHWH??_@gACCCAAAE?C@E@{B`AC@AD?BAB@B?D\\`A??QHWFK@I@KAIAiAd@GBq@Z??j@`B??"
//                    val route = "izwxCmmiyHy@\\_A^??Oe@??bCaA~As@d@W??Vv@jCbIXz@HT??MDoBv@cLzE_Bp@YNKDwAj@sB|@sAj@wItDo@VqMlF??oA`AwErBSHEBCBCFCDADAD??HVd@|A~Qfl@tJl[??RZvB|GDN@FBBDDDBBBF@??NGj@UbIoDr@]nHcDFCfAg@|JiELG~EwBdAc@??Tv@nDtMtA`FPn@Nh@J\\hAfE???TATCNIXIV_EhCw@f@s@b@MHaAn@c@Vi@\\UNaAn@??"


//                    val route: String = "k__yCqjiyHMHi@`@kCrB??eBiBgBkBa@e@_BeBsAuAgAoACEEGAIAC?I@Y??RSjD{Cb@[l@k@HKVWJKPQNOXa@hB_BZYpAiAhAeAh@e@HItBkBj@i@??t@o@RQ\\WPQNOb@_@pF}ElF{Ed@c@fAgAd@a@x@y@Z[lBeB??aAcA??_@a@{AcBy@}@kAsAcEuFwEgG_AoAg@k@a@a@g@]oAy@aAo@o@k@[YY[SUUU}@gA{AeBq@w@e@k@U[??c@]QSy@gAg@m@c@i@][a@[??}@mAqD_FwAmBoAuAqAqAk@g@q@m@eCaBmBcAyAm@}@_@y@WqA_@_AQ}A[q@I[Ec@EqBMiBCmIGgLCeH?mB?gB?q@?u@?yE?oB?o@?qEEk@]W???KIGKEKMuA??bBW`@CtA???"
//                    val route: String = "a{wxCabdyHh@bAbBMgCbBo@I]QUS_@mA{@oC}A{EoA}D_CcIkAqCw@eCMKGCmAd@gJpDKAg@Ba@Ge@Mc@[wB}G{CkIwBmF[q@[c@}D{LuB_HkEaNkEkNoCeJaAsCsCwHo@wAiCsEyA}ByD{EyG_GoL_JsLsJwScPiA_Aq@c@yH}FwFeE_JsHkC{By@m@gBoBkF}F_EkF_CyCyCkDkGqGaBsB{EiGeFgHaI}JiBuBi@_@qCuB{BkA_Ae@uEaBmAY_Co@a@Gc@GgAMcBIeK@iKDgOBoICkGAk@Bu@Kk@G}AAqG?cC@{@?aLF{Z?oBCaCOq@a@EGM]GaANiE@uBDaL?sCIWEoC@mC@kECIOE?x@M^I|@GROJiCCiDB}C@aCH{Ej@m@B_ANc@D_@BOEWCAAICOBILANFJB@B^_@lEQbDWnE"
//                val route:String = "a{wxCabdyHh@bAbBMgCbBo@I]QUS_@mA{@oC}A{EoA}D_CcIkAqCw@eCMKGCmAd@gJpDKAg@Ba@Ge@Mc@[wB}G{CkIwBmF[q@[c@}D{LuB_HkEaNkEkNoCeJaAsCsCwHo@wAiCsEyA}ByD{EyG_GoL_JsLsJwScPiA_Aq@c@yH}FwFeE_JsHkC{By@m@gBoBkF}F_EkF_CyCyCkDkGqGaBsB{EiGeFgHaI}JiBuBi@_@qCuB]Qe@e@a@_@MSISCY?[?MLk@T]PIl@OV@RBXJZZRb@BZ?b@[|@{CpGaAhBg@z@cAjBkCdE}D~EgBjByEfDcBdAmCnBoFzDaJxG{CpB}MjJeGzEwAbAoChBoDzBuBbByArAs@t@{AdBq@|@[b@q@rAwAlC]h@oAvC_BlDe@|@iC~DcC~CeBjBqBjB_@ViDfCyG|D{@f@oI|EyCzAoO`JqUhNuZzQmBhA{TbNwPhKuWdPsDnCmBbBcBbBgGbHwEdFk@d@{ApAyAdAyA~@oBbAuAj@y@XkCv@sCj@sFn@mCZ{QnBmFj@qBZeQlBe_@xE}IbA}Hz@{PxBiGr@iMtAiBZgE|@qA^oBj@qBt@cE`BaIrDwB~@uGfCiKbEaCfAkC~@{EnByAl@cBv@cRnHgOhG_E~AeFlB_P`GeG|BoDxAgAf@aBl@aFvBoHhDaFbBiHvC{FrBgEhBkH|CiG`CkHtCsJvDmH~CqBt@eCdAgNlF_@PcFtBiJnDmShIwP`Hm^vNuB~@cBv@mAd@cQzGk@TqWhK_JzDcRjHkDvAwMjF_DnA}^bOsuBvz@qp@dXwMjF}FtBmJ~CgJxCeF`AgLdCyO~BwLlA{E^yDViKb@yOn@ic@xAa[hAwXdA}i@nB_K^_KTcKb@iPl@aBHmMTcTHwKGaLQqM[kGY}CQaPeAqIm@kKq@}BQiAIw\\\\yBye@{Cs\\\\yBoo@kEq\\\\yBkHc@uAMwNaA_E_@{TsAwL{@mWeBgKs@ov@}EyEW}CEgHOiECmBBkJH}DFwERwBJaDRmETeNtA_D\\\\kMlBoEt@_TnDcY~EmFx@u_@pGuU~DuStDmU|DiiApRcc@pHyT~DuQrCsCn@eS`DuT|DeY~EmKbBSEY?cAFo@AwASsAa@wA{@mA{@aA_@}@Sy@IyAAUGUQKKMGQ_@Oy@GIM{@"
//                val route:String = "a{wxCabdyHo@_BAi@\\\\SgAgC_AuBVSy@eB_AByAj@Og@kEuM_AuBsOxGoFbCcH~Bw^xOWYg@@U`@qR~IsGlCoFrAmAG}@v@uEp@qDNsHHmHSgDc@][mB?{r@{IyI}@eB?wDPoDr@_HdDGCYBIFaOqY}BqEaAh@yAdA}AuByBjBQAyEcHm@cAQE{@x@iARgA`AoAfAQKIGa@k@gByCQKuImOiHmKwGoLeAwBmCr@mI`DyIpDmAj@sAcCoBwDi@N_CqEc@{@uAy@mAKo@LaD|BcAC_F}Gg@AcRrIgF~@gGnByItD}DdBg@d@e@?kBFyFNwBf@y@\\\\y@B_DoKcFaOgD{HuAqC_@MgBsDsAoC{EwHkImJcE{C_FoCgI}Aoh@qFuAMwCe@iLqAqGcAmKgDeEoBaC_BkDcDyByC_DqEwAgCe@M}Az@{MvHoG~CiFfBoGrAsI`C}GrCyFvCyDvAgFlAsI^iGKoIiBaJgDsB{A_KsJg\\\\e[}EgEeCwAsGyBcHg@ig@ByQCkL@kJn@}FbBuDtBmCbC{OjQsJ~KeL|LyEdDeCbAyFjA{ANeC@s[?iECsCc@wAs@oBkBoAwAyCgBeCu@iEa@oDTaJdAqFOsD{@iDuA{EiB_Ca@gLkE}H_AeACSFyFEwQNwOrAeQ`EaOpFeG`Cm]nPoa@`RiLnEgLzCqLtBkPnC_g@fHq_@vEic@rGksB`Zwq@tJiO~Bo_@fHc_@|IeN`EmXpIgIxCO`@Kr@@l@DVgElBwE|BqFpCeIdEuFxCkFnCsRvJwyAhv@i`@xRgOnGmTbHgPjEoUpEmW`DyLl@cCO_KeAkO^gQh@mK`@uGOkMcB{GkAwJ}AmJ{BwLsEaFqA{HqBeMgEeIiDwKeCoFyBgDcCwFkCqIkBiKgEqGyCwGuDuCsB}FoCgEcAoH_Cip@}Zaf@aU{ZuMgX{JiOcFeMaEwHyBiCCcER{FeAmD}BkDuD_EuC_FoBqIiB_PiFsl@cRgg@mPe\\\\kKqMcEqGyBuDi@{EQqE\\\\iGv@kBBSAYAa@dHK`AMf@mAa@GCC?IFi@rBgAhEeA]mADw@l@_DtIBd@E`@mB~E}F`Ry@z@gAB{Ac@cAWq@r@c@d@yA|G_@xAm@`C}A`GsD|NgD|Nm@hCaA|IFxKjBlf@]lE}B~EeDfC}C~AO|@m@`@U@Vx@hCbInCxFxI|Wz@tDjHzTjM`_@lCbGzG`LbUnW{AjDc@r@_CtD}AfC}CbFiHjL}Sl\\\\{BhDa@n@GfBtBnBbDlDjCpE~B|F~AzGbJ|p@rC~SrAxJ"


//                val route: String ="a{wxCabdyHh@bAbBMgCbBo@I]QUS_@mA{@oC}A{EoA}D_CcIkAqCw@eCMKGCmAd@cM~Eg@RmElB{MdGgJ|DyBlAeCbA_CfAw@ZS?OCQCSBMJKT?PBRJNRHTANGJQB[AIjAk@lEoB"

                    //Office to home
//                val route ="g~wxCgcdyHRf@YNQHOc@k@kB_@oA}BaHoCkJkAqCs@{BIOOIuLvEa@Ac@@YG[Kc@[wB}G_CsGu@kBy@qB_AyB[c@_CkHeDkKmA_EiImW}CiKaB{EuBuFe@cAiDeGaAyAwBqCY[oAoAw@u@iC}BgBwAgJeHmH}FuCaCyFmEaDeCeIoG_D}BsL}ImHcGw@q@yBkB[Yy@m@s@w@mAsAqEaF{DgFkAwAkDgEwGaHuAaBmAyAsD{EmCyDyFoHaCwC{BmCe@a@kCoBs@a@aCqAqFoBmEiA_BWwBMuJA}GDqLB_E?yF?mKCk@Ba@Eo@Ki@AiIAkABqAA{HDgP@eGA{G@cBGsAKi@YMOKUGc@Ae@FaBF}BD_J@}G?WEIEa@AaA?cC@iGAMGEIA?x@UdAAZGNOJ_FAoD@eDHsCVsCXcAN_AH[GMCGCG?OHEN@LJF@^QvBObCm@pJMp@]E"
                Log.e("mapApplication",route)
                    mapApplication?.stopListenerThing()
                    mapApplication?.stopListenerThing()
//                    mapApplication?.clearRoute()
                    getCurrentLocation({ location ->
                        mapApplication?.startNavigation(
                            Point.fromLngLat(
                                location.longitude,
                                location.latitude
                            ), route, this
                        )
                        startTimer()
                    })
                    Log.d("MapApplication", "======= Route -> ")
//                    Toast.makeText(applicationContext, route,Toast.LENGTH_SHORT).show()
//                }
            }

        }, 1000)
    }

    private fun startServices() {
        startForegroundService(Intent(this, BackgroundLocationService::class.java))
    }

    private fun requestLocationPermission() {
        requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun initMap() {
        mapApplication =
            MapApplication(binding.navigationView, binding.maneuverView, binding.btnOffRoute, binding.btnDrawNextLayout)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
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

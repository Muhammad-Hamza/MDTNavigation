package com.karwa.mdtnavigation

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.logger.Logger
import com.google.gson.Gson
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import com.karwa.mdtnavigation.log.FirebaseLogger
import com.karwa.mdtnavigation.model.ChunkModel
import com.karwa.mdtnavigation.model.getDistance
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.matching.v5.MapboxMapMatching
import com.mapbox.api.matching.v5.models.MapMatchingMatching
import com.mapbox.api.matching.v5.models.MapMatchingResponse
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.*
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.TripSessionResetCallback
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.trip.session.*
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.*
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


class MapApplication constructor(
    var mapView: MapView,
    var maneuverView: MapboxManeuverView?,
    var offRouteButton: Button,
    var nextRouteButton: Button,
    var wazeButton: RelativeLayout,
    var logger: FirebaseLogger,
    val onDone: () -> Unit
) : KLocationObserver {

    private val gson = Gson()

    //Coroutine
    private val offRouteScope = CoroutineScope(Dispatchers.IO)
    private var offRouteJob: Job? = null
    private var isHitFirstTime = false
    private var OFF_ROUTE_TIMER = 8000L

    private var isOffRouteTimerInProgress = false
    private var calculationScope = CoroutineScope(Dispatchers.IO)
    private val routeScope = CoroutineScope(Dispatchers.IO)

    //    private var currentRoutelinesStr = ""
    private var currentPathDrawingList = mutableListOf<Point>()
    private var ACCEPT_TOLERENCE = 10//10 meter

    private var isAddedTheLastPointIndex = false
    private var destination: Point? = null

    var isNavigationInProgress = false
    var navigationRouteId: Long? = null
    private var isFirstTime = true
    private var mapCameraRecenterTimer = 0L
    private val MAPBOX_DELAY_TIMER: Long = 5000
    private var lastOffrouteTime: Long = 0
    private val OFF_ROUTE_RETRY: Long = 5000
    val MAPBOX_ACCESS_TOKEN = mapView.context.getString(R.string.mapbox_access_token)

    var lastCurrentLocation: LatLng? = null
    var lastSpeed: Float = 0f
    private var lastDestinationCurrentPoint: Point? = null
    private var lastDestinationOverallPoint: Point? = null
    private lateinit var context: Context

    private var isOffRoutingFirstTime = false
    private var isRoutingCreatingFirstTime = false

//    private var offRouteCount = 0;

    /**
     * [NavigationLocationProvider] is a utility class that helps to provide location updates generated by the Navigation SDK
     * to the Maps SDK in order to update the user location indicator on the map.
     */
    private val navigationLocationProvider = NavigationLocationProvider()


    /**
     * Mapbox Navigation entry point. There should only be one instance of this object for the app.
     * You can use [MapboxNavigationProvider] to help create and obtain that instance.
     */
    private lateinit var mapboxNavigation: MapboxNavigation

    /**
     * Produces the camera frames based on the location and routing data for the [navigationCamera] to execute.
     */
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    /**
     * Used to execute camera transitions based on the data generated by the [viewportDataSource].
     * This includes transitions from route overview to route following and continuously updating the camera as the location changes.
     */
    private lateinit var navigationCamera: NavigationCamera

    /**
     * Debug tool used to play, pause and seek route progress events that can be used to produce mocked location updates along the route.
     */
    private val mapboxReplayer = MapboxReplayer()

    /**
     * Debug tool that mocks location updates with an input from the [mapboxReplayer].
     */
    private val replayLocationEngine = ReplayLocationEngine(mapboxReplayer)

    /**
     * Debug observer that makes sure the replayer has always an up-to-date information to generate mock updates.
     */
    private val replayProgressObserver: ReplayProgressObserver =
        ReplayProgressObserver(mapboxReplayer)

    /**
     * Generates updates for the [routeArrowView] with the geometries and properties of maneuver arrows that should be drawn on the map.
     */
    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi()

    /**
     * Generates updates for the [routeLineView] with the geometries and properties of the routes that should be drawn on the map.
     */
    private lateinit var routeLineApi: MapboxRouteLineApi

    /**
     * Draws route lines on the map based on the data from the [routeLineApi]
     */
    private lateinit var routeLineView: MapboxRouteLineView

    /**
     * Extracts message that should be communicated to the driver about the upcoming maneuver.
     * When possible, downloads a synthesized audio file that can be played back to the driver.
     */
    private lateinit var speechApi: MapboxSpeechApi

    private var haveDeparted = false


    private var onCameraIdleListener: OnCameraIdleListener? = null


    lateinit var listOfChunks: ArrayList<ChunkModel>
    lateinit var currentList: MutableList<Point>
    var currentIndex = -1
    var isOffRoute = false

    private var previousPoint: Point? = null // Keep track of the previous location

    /**
     * Gets notified with progress along the currently active route.
     */
    private val routeProgressObserver: RouteProgressObserver =
        RouteProgressObserver { routeProgress -> // update the camera position to account for the progressed fragment of the route
            logger.logSelectContent(
                "Route Progress", "Update", "Fraction traveled: ${routeProgress.fractionTraveled}"
            )

            val fractionTraveled = routeProgress.fractionTraveled
            updateProgressOnTheMap(routeProgress)
            val arrival = Calendar.getInstance()
            arrival.add(Calendar.SECOND, routeProgress.durationRemaining.toInt())

//            if (lastSpeed > 0) {
            updateApplicationState(routeProgress, arrival)
//            }
            if (lastCurrentLocation != null) {
                val distanceInMeters = SphericalUtil.computeDistanceBetween(
                    LatLng(
                        lastCurrentLocation!!.latitude, lastCurrentLocation!!.longitude
                    ), LatLng(destination!!.latitude(), destination!!.longitude())
                )

                Log.e("mapApplication", "Remaining Distance: " + distanceInMeters)

//                if (distanceInMeters < 150.0) {
//                    if (!isLastRound()) {
//                        if (nextRouteButton.visibility == View.GONE)
//                            nextRouteButton.visibility = View.VISIBLE
//                    } else {
//                        if (nextRouteButton.visibility == View.VISIBLE)
//                            nextRouteButton.visibility = View.GONE
//                    }
//                } else {
//                    nextRouteButton.visibility = View.GONE
//                }
                if (isLastRound()) {
                    if (fractionTraveled >= 0.96 || distanceInMeters < 50.0) {
                        startNextRoute(true)
                        if (wazeButton.visibility == View.VISIBLE) {
                            hideWazeButton()
                        }

                        mapboxNavigation.historyRecorder.stopRecording({})
                    }
                } else {
                    if (fractionTraveled >= 0.80) {
                        if (isOffRoute) {
                            isOffRoute = false
                        }
                        if (wazeButton.visibility == View.VISIBLE) {
                            hideWazeButton()
                        }
//                        if (isLastRound()) {
//                            if (fractionTraveled >= 0.96) {
//                                startNextRoute(true)
//                            }
//                        } else {
                        startNextRoute(true)
//                        }
                    }
                }
            }
        }

    private fun updateApplicationState(routeProgress: RouteProgress, arrival: Calendar) {
        logger.logSelectContent(
            "App State",
            "Route Progress",
            "Route progress state update -> remaining distance: ${routeProgress.distanceRemaining}"
        )
        if (routeProgress.currentState == RouteProgressState.COMPLETE || routeProgress.fractionTraveled >= 1.0) {
            ApplicationStateData.getInstance().txtArrivalTime = ("--:--")
            ApplicationStateData.getInstance().arrivalTime = (0)
            ApplicationStateData.getInstance().txtRemainingDistance = ("--")
            ApplicationStateData.getInstance().txtRemainingTime = ("--")
            ApplicationStateData.getInstance().setEtaToStop(0.0)

        } else {
            if (isLastRound()) {
                logger.logSelectContent("ETA", "LAST Round", gson.toJson(routeProgress))
                val remainingDistance = formatDistance(routeProgress.distanceRemaining.toDouble())
                logger.logSelectContent("ETA", "Remaining Distance", remainingDistance)
                ApplicationStateData.getInstance().txtRemainingDistance = remainingDistance

                val remainingTime = formatTime(routeProgress.durationRemaining)
                logger.logSelectContent("ETA", "Remaining Time", remainingTime)
                ApplicationStateData.getInstance().txtRemainingTime = (remainingTime)

                ApplicationStateData.getInstance().setEtaToStop(routeProgress.durationRemaining)
                ApplicationStateData.getInstance().arrivalTime = (arrival.timeInMillis)
                ApplicationStateData.getInstance().txtArrivalTime = (String.format(
                    "%1$02d:%2$02d", arrival.get(Calendar.HOUR_OF_DAY), arrival.get(Calendar.MINUTE)
                ))
            } else {
                logger.logSelectContent("ETA", "Non Last Round", gson.toJson(routeProgress))
                val totalRemainingDistance =
                    routeProgress.distanceRemaining.toDouble() + listOfChunks.getDistance(isOffRoute)
                val remainingDistance = routeProgress.distanceRemaining.toDouble()

                logger.logSelectContent(
                    "ETA",
                    "Remaining Chunk Distance",
                    totalRemainingDistance.toString()
                )
                logger.logSelectContent("ETA", "Remaining Distance", remainingDistance.toString())

                ApplicationStateData.getInstance().txtRemainingDistance =
                    (formatDistance(remainingDistance + totalRemainingDistance))
                logger.logSelectContent(
                    "ETA",
                    "Total Distance",
                    ApplicationStateData.getInstance().txtRemainingDistance
                )

                val remainingTimeInHour =
                    calculateTravelTime(totalRemainingDistance, lastSpeed.toDouble())

                logger.logSelectContent("ETA", "Speed", lastSpeed.toString())
                logger.logSelectContent("ETA", "Remaining Time", formatTime(remainingTimeInHour))

                ApplicationStateData.getInstance().txtRemainingTime =
                    (formatTime(remainingTimeInHour))

                ApplicationStateData.getInstance().setEtaToStop(remainingTimeInHour)

                val arrivalTimeCalendar = Calendar.getInstance()

                arrivalTimeCalendar.timeInMillis =
                    System.currentTimeMillis() + (remainingTimeInHour * 1000).toLong()
                logger.logSelectContent(
                    "ETA",
                    "Remaining Arrival Time",
                    "" + arrivalTimeCalendar.timeInMillis
                )

                val newTime = String.format(
                    "%1$02d:%2$02d",
                    arrivalTimeCalendar.get(Calendar.HOUR_OF_DAY),
                    arrivalTimeCalendar.get(Calendar.MINUTE)
                )
                ApplicationStateData.getInstance().arrivalTime = (arrivalTimeCalendar.timeInMillis)
                ApplicationStateData.getInstance().txtArrivalTime = newTime
                logger.logSelectContent("ETA", "Arrival Time", newTime)
            }
        }
    }

    private fun updateProgressOnTheMap(routeProgress: RouteProgress) {
        val style = mapView.getMapboxMap().getStyle()
        if (style != null) {
            val maneuverArrowResult = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
            routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)

            routeLineApi.updateWithRouteProgress(routeProgress) {
                routeLineView.renderRouteLineUpdate(style, it)
            }
        } // update top banner with maneuver instructions
        val maneuvers = maneuverApi.getManeuvers(routeProgress)
        maneuvers.fold({ error ->

        }, {
            CoroutineScope(Dispatchers.Main).launch {
                maneuverView?.visibility = View.VISIBLE
                maneuverView!!.renderManeuvers(maneuvers)
            }
        })
    }

    /**
     * Gets notified whenever the tracked routes change.
     *
     * A change can mean:
     * - routes get changed with [MapboxNavigation.setRoutes]
     * - routes annotations get refreshed (for example, congestion annotation that indicate the live traffic along the route)
     * - driver got off route and a reroute was executed
     */
    var drawFirstTime = true
    private val routesObserver: RoutesObserver = RoutesObserver { routeUpdateResult ->
        if (routeUpdateResult.navigationRoutes.isNotEmpty()) { // generate route geometries asynchronously and render them
            if (drawFirstTime) {
                drawFirstTime = false
                val routeLines =
                    routeUpdateResult.navigationRoutes.map { NavigationRouteLine(it, null) }

                routeLineApi.setNavigationRouteLines(routeLines) { value ->
                    mapView.getMapboxMap().getStyle()?.apply {
                        routeLineView.renderRouteDrawData(
                            this, value
                        )
                    }
                }
                viewportDataSource.onRouteChanged(routeUpdateResult.navigationRoutes.first())
                viewportDataSource.evaluate()
            }
        } else {
            logger.logSelectContent("RoutesObserver", "No Route", "No navigation route found")
            clearRoutesAndArrow()
        }

    }

    fun clearRoutesAndArrow() { // remove the route line and route arrow from the map
        val style = mapView.getMapboxMap().getStyle()
        if (style != null) {
            routeLineApi.clearRouteLine { value ->
                routeLineView.renderClearRouteLineValue(
                    style, value
                )
            }
            routeArrowView.render(style, routeArrowApi.clearArrows())
        }
        viewportDataSource.clearRouteData()
        viewportDataSource.evaluate()
    }

    /**
     * When a synthesized audio file was downloaded, this callback cleans up the disk after it was played.
     */
    private val voiceInstructionsPlayerCallback =
        MapboxNavigationConsumer<SpeechAnnouncement> { value -> // remove already consumed file to free-up space
            speechApi.clean(value)
        }

    /**
     * Based on whether the synthesized audio file is available, the callback plays the file
     * or uses the fall back which is played back using the on-device Text-To-Speech engine.
     */
    private val speechCallback =
        MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
            expected.fold({ error -> // play the instruction via fallback text-to-speech engine
                voiceInstructionsPlayer.play(
                    error.fallback, voiceInstructionsPlayerCallback
                )
            }, { value -> // play the sound file from the external generator
                if (isNavigationInProgress) voiceInstructionsPlayer.play(
                    value.announcement, voiceInstructionsPlayerCallback
                )
            })
        }

    /**
     * Observes when a new voice instruction should be played.
     */
    private val voiceInstructionsObserver: VoiceInstructionsObserver =
        VoiceInstructionsObserver { voiceInstructions ->
            if (isNavigationInProgress) speechApi.generate(voiceInstructions, speechCallback)
        }

    /*
    *
    *
     */
    private val arrivalObserver = object : ArrivalObserver {
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
//            Log.d("MapApplication", "Arrived at the final destination.")
            // You can handle other arrival-related tasks here
        }

        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
//            Log.d("MapApplication", "Next route leg started.")
        }

        override fun onWaypointArrival(routeProgress: RouteProgress) {
//            Log.d("MapApplication", "Arrived at waypoint.")
        }
    }

    /**
     * Plays the synthesized audio files with upcoming maneuver instructions
     * or uses an on-device Text-To-Speech engine to communicate the message to the driver.
     */
    private lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer

    /**
     * Draws maneuver arrows on the map based on the data [routeArrowApi].
     */
    private lateinit var routeArrowView: MapboxRouteArrowView

    /**
     * Generates updates for the [MapboxManeuverView] to display the upcoming maneuver instructions
     * and remaining distance to the maneuver point.
     */
    private lateinit var maneuverApi: MapboxManeuverApi

    /**
     * Generates updates for the [MapboxTripProgressView] that include remaining time and distance to the destination.
     */
    private lateinit var tripProgressApi: MapboxTripProgressApi

//    private fun addOffRouteDelay() {
//        offRouteScope.launch {
////            isOffRouteTimerInProgress = true
////            delay(OFF_ROUTE_TIMER)
////            isOffRouteTimerInProgress = false
////            if (showAction) {
//
////            val needToShowRoute = isWithinTolerance()
////
////            if (!needToShowRoute) {
//            withContext(Dispatchers.Main) {
//                handleOffRouteDetected()
//            }
////            }
////            }
//        }
//    }

    fun decodePolyline(polyline: String): List<Point> {
        return PolylineUtils.decode(polyline, 6)
    }

    fun isWithinTolerance(): Boolean {
        if (lastCurrentLocation != null && currentPathDrawingList.size > 0) {

            for (i in 0 until currentPathDrawingList.size - 1) {

                val distance = TurfMeasurement.distance(
                    Point.fromLngLat(
                        lastCurrentLocation!!.longitude,
                        lastCurrentLocation!!.latitude
                    ), currentPathDrawingList[i]
                )
                if (distance <= ACCEPT_TOLERENCE) {
                    logger.logSelectContent("OFFSET", "Distance", "DISTANCE-> " + distance)
                    return true
                }
            }
            return false
        }
        return true
    }

    private val offRouteProgressObserver: OffRouteObserver = OffRouteObserver { isOffRoute ->
        val offset: Long = System.currentTimeMillis() - lastOffrouteTime
        Log.e("OFFSET", "OFFSET_duration -> $offset, isOffRoute: $isOffRoute")

        if (isOffRoute) {
            if (!isHitFirstTime) {
                isHitFirstTime = true
                logger.logSelectContent(
                    "Off Route", "OffRoute Detected", "Initial offRoute detected"
                )
                addOffRouteDelay(false)
            } else {
                if (!isOffRouteTimerInProgress && offRouteButton.visibility == View.GONE) {
                    logger.logSelectContent(
                        "Off Route", "OffRoute Detected", "Vehicle went off-route"
                    )
                    addOffRouteDelay(showAction = true)
                }
            }
        }



        /*        if (isOffRoute) {
                    if (lastCurrentLocation != null) {
                        if (!isHitFirstTime) {
                            // First-time detection: wait for 15 seconds
                            if (offset > OFF_ROUTE_TIMER) {
                                lastOffrouteTime = System.currentTimeMillis()
                                isHitFirstTime = true
                                Log.e("OFFSET", "OFFSET-> First IF Condition")
                                logger.logSelectContent(
                                    "Off Route", "OffRoute Detected", "Initial offRoute detected"
                                )
                            }
                        } else {
        //                    isHitFirstTime==true here
                            // After the initial ignore period
                            if (offset > OFF_ROUTE_TIMER && offRouteButton.visibility == View.GONE) {
                                Log.e("OFFSET", "OFFSET-> Subsequent Else Condition")
                                lastOffrouteTime = System.currentTimeMillis()
                                logger.logSelectContent(
                                    "Off Route", "OffRoute Detected", "Vehicle went off-route"
                                )
                                addOffRouteDelay()
                            }
                        }
                    }
                }
                else {
                    // When the vehicle is no longer off-route

                    *//*            if (offRouteButton.visibility == View.VISIBLE) {
                Log.e("OFFSET", "OFFSET-> Main Else Condition")
//                if (isWithinTolerance()) {
                    Log.e("OFFSET", "OFFSET-> Main Else Condition After Tolerance True")
                    logger.logSelectContent(
                        "Off Route",
                        "OffRoute Detected",
                        "OffRoute-False, Within Tolerance, hide button"
                    )
                    offRouteButton.visibility = View.GONE
//                }
            }*//*
        }*/
    }

    private fun addOffRouteDelay(showAction: Boolean = false) {
        offRouteScope.launch {
            isOffRouteTimerInProgress = true
            delay(OFF_ROUTE_TIMER)
            isOffRouteTimerInProgress = false

            if (showAction) {
                withContext(Dispatchers.Main) {
//                    handleOffRouteDetected()
                    increaseOffRouteCount()
                }
            }
        }
    }
    /*
        private val offRouteProgressObserver: OffRouteObserver = OffRouteObserver { isOffRoute ->
            val offset: Long = System.currentTimeMillis() - lastOffrouteTime
            Log.e("OFFSET","OFFSET->"+offset+", OffRoute: "+isOffRoute)
            if (isOffRoute) {
                if (lastCurrentLocation != null) {
                    if (!isHitFirstTime) {
                        if (offset > OFF_ROUTE_TIMER) {
                            lastOffrouteTime = System.currentTimeMillis()
                            Log.e("OFFSET","OFFSET->IF Condition")
                            isHitFirstTime = true
                            logger.logSelectContent(
                                "Off Route", "OffRoute Detected", "Initial offRoute detected"
                            )
                        }
                    } else {
                        if (offset > OFF_ROUTE_TIMER && offRouteButton.visibility == View.GONE) {
                            Log.e("OFFSET","OFFSET->Else Condition")
                            lastOffrouteTime = System.currentTimeMillis()
                            logger.logSelectContent(
                                "Off Route", "OffRoute Detected", "Vehicle went off-route"
                            )
                            addOffRouteDelay()
                        }
                    }
                }
            } else {
                if (offRouteButton.visibility == View.VISIBLE) {
                    Log.e("OFFSET","OFFSET->Main Else COndition")
                    if (isWithinTolerance()) {
                        Log.e("OFFSET","OFFSET->Main Else COndition After Tolerance True")
                        logger.logSelectContent(
                            "Off Route",
                            "OffRoute Detected",
                            "OffRoute-False, Within Tolerence, hide button"
                        )
                        offRouteButton.visibility = View.GONE
                    }
                }
            }
        }
    */

    private fun handleOffRouteDetected() {
        cancelOffRouteTimer()
        offRouteButton.performClick()
        offRouteButton.visibility = View.VISIBLE
    }

    private fun cancelOffRouteTimer() {
        offRouteJob?.cancel()
    }


    init {

        initMapboxNavigation()

        initPuckLocation()

        initCamera()

        // initialize maneuver arrow view to draw arrows on the map
        val routeArrowOptions =
            RouteArrowOptions.Builder(ApplicationStateData.getInstance().applicationContext).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)

        // initialize route line, the withRouteLineBelowLayerId is specified to place
        // the route line below road labels layer on the map
        // the value of this option will depend on the style that you are using
        // and under which layer the route line should be placed on the map layers stack
        val mapboxRouteLineOptions =
            MapboxRouteLineOptions.Builder(ApplicationStateData.getInstance().applicationContext)
                .withRouteLineBelowLayerId(
                    "road-label"
                )

                .build()
//
//        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(context)
//            .withRouteLineBelowLayerId("road-label")
//            .withRouteLineResources(RouteLineResources.Builder()
//                .routeLineColorResources(RouteLineColorResources.Builder().routeDefaultColor(Color.RED).build())
//                .restrictedRoadLineWidth(5.0)
//                .build())
//            .build()
        routeLineApi = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

        // make sure to use the same DistanceFormatterOptions across different features
        val distanceFormatterOptions = mapboxNavigation.navigationOptions.distanceFormatterOptions

        // initialize maneuver api that feeds the data to the top banner maneuver view
        maneuverApi = MapboxManeuverApi(MapboxDistanceFormatter(distanceFormatterOptions))

        initVoiceInstructions()

        // initialize bottom progress view
        tripProgressApi = MapboxTripProgressApi(
            TripProgressUpdateFormatter.Builder(ApplicationStateData.getInstance().applicationContext)
                .distanceRemainingFormatter(
                    DistanceRemainingFormatter(distanceFormatterOptions)
                )
                .timeRemainingFormatter(TimeRemainingFormatter(ApplicationStateData.getInstance().applicationContext))
                .percentRouteTraveledFormatter(
                    PercentDistanceTraveledFormatter()
                ).estimatedTimeToArrivalFormatter(
                    EstimatedTimeToArrivalFormatter(
                        ApplicationStateData.getInstance().applicationContext,
                        TimeFormat.NONE_SPECIFIED
                    )
                ).build()
        )

        setupVoiceButton()
    }

    private fun initVoiceInstructions() {
        speechApi = MapboxSpeechApi(
            ApplicationStateData.getInstance().applicationContext,
            MAPBOX_ACCESS_TOKEN,
            Locale.US.toLanguageTag()
        )

        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
            ApplicationStateData.getInstance().applicationContext,
            MAPBOX_ACCESS_TOKEN,
            Locale.US.toLanguageTag()
        )
    }

    private fun setupVoiceButton() {
//        voiceToggleButton.setOnCheckedChangeListener { _, isChecked ->
//            voiceInstructionsPlayer.volume(SpeechVolume(if (!isChecked) 1.0f else 0.0f))
//        }
    }

    private fun calculateOverallRide() {

    }

    public fun startNavigation(destination: Point?, encodedPath: String, context: Context) {
        try {
            destination?.let {
                if (lastCurrentLocation == null && destination != null) {
                    lastCurrentLocation = LatLng(destination.latitude(), destination.longitude())
                }
                navigateToFixedRoute(it, encodedPath, context)
            }
            isNavigationInProgress = true
        } catch (e: Exception) {
            logger.logSelectContent("startNavigation", "Exception", e.message!!)
            e.printStackTrace()
        }
    }

    fun openWazeApp() {
        // Waze URI for navigation from current location to a destination
//        val wazeUri =
//            "waze://?ll=${lastDestinationOverallPoint?.latitude()},${lastDestinationOverallPoint?.longitude()}&navigate=yes"

        val lastDestinationLat = lastDestinationOverallPoint?.latitude()
        val lastDestinationLng = lastDestinationOverallPoint?.longitude()

        val startLat = lastCurrentLocation?.latitude
        val startLng = lastCurrentLocation?.longitude

        if (lastDestinationLat != null && lastDestinationLng != null && startLat != null && startLng != null) {
            val wazeUrl =
                "https://waze.com/ul?ll=$lastDestinationLat,$lastDestinationLng&from=$startLat,$startLng&navigate=yes"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(wazeUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        //
//        if (intent.resolveActivity(context.packageManager) != null) {
//            context.startActivity(intent)
//        } else {
//            val playStoreUri = "https://play.google.com/store/apps/details?id=com.waze"
//            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUri))
//            context.startActivity(playStoreIntent)
//        }
    }

    private fun drawSimplePolyline(list: List<Point>) {

        val lineString = LineString.fromLngLats(list)

        val sourceId = "polyline-source"
        val geoJsonSource = geoJsonSource(sourceId) {
            geometry(lineString)
        }

        mapView.getMapboxMap().getStyle { style ->
            style.addSource(geoJsonSource)

            val lineLayer = LineLayer("polyline-layer", sourceId).apply {
                lineColor("#B7D6F6")  // Set the polyline color
                lineWidth(10.0)        // Set the polyline width
            }

            logger.logSelectContent("Polyline", "Draw", "Drawing polyline on the map")

            if (style.styleLayerExists("mapbox-masking-layer-main")) {
                style.addLayerBelow(lineLayer, "mapbox-masking-layer-main")
            } else {
                style.addLayer(lineLayer)
            }

//            style.addLayerBelow(lineLayer, "mapbox-masking-layer-main")
        }

    }

    @SuppressLint("MissingPermission")
    fun navigateToFixedRoute(destination: Point, encodedPath: String?, context: Context) {
        this.context = context

        stopListenerThing()

//        val list = DummyContent.listOfTTestLatLng()

        val list = PolyUtil.decode(encodedPath)

        val finalList = mutableListOf<Point>()
        for (i in 0 until list.size - 1) {
            finalList.add(Point.fromLngLat(list[i].longitude, list[i].latitude))
        }

        lastDestinationOverallPoint = finalList.last()
        drawSimplePolyline(finalList)

        addMarker(finalList.last())

//        startLocationTracking(finalList)
        currentIndex = -1
        isOffRoute = false

        Log.e("FILTER", "COMING SIZE: " + finalList.size)

        listOfChunks = ArrayList(finalList.chunked(15).map {
            ChunkModel(
                list = ArrayList(it), linearDistanceInMeter = calculateTotalHaversineDistance(it)
            )
        })
        Log.e("FILTER", "LIST OF CHUNK SIZE: " + listOfChunks.size)

        calculateOffRouting()
    }

    private fun startLocationTracking(list: List<Point>) {
        currentIndex = -1
        isOffRoute = false

        Log.e("FILTER", "COMING SIZE: " + list.size)

        listOfChunks = ArrayList(list.chunked(15).map {
            ChunkModel(
                list = ArrayList(it), linearDistanceInMeter = calculateTotalHaversineDistance(it)
            )
        })
        Log.e("FILTER", "LIST OF CHUNK SIZE: " + listOfChunks.size)

        updateListRoute(false, {})

    }

    private fun updateListRoute(
        needToAddLocationAtInitial: Boolean = true, onSuccessfullDraw: () -> Unit
    ) {
        routeScope.launch {
            if ((currentIndex + 1) < listOfChunks.size) {

                logger.logSelectContent(
                    "Route", "Update List", "Updating route list for the next segment"
                )

                currentIndex = currentIndex + 1
                lastDestinationCurrentPoint = listOfChunks.get(currentIndex).list.last()

                currentList = listOfChunks.get(currentIndex).list
//            Log.e("mapApplication","Size: "+listOfChunks.size)
//            Log.e("mapApplication","Size: "+currentList.size)
                withContext(Dispatchers.Main) {
                    if (currentIndex == listOfChunks.size - 1) {
                        wazeButton.visibility = View.VISIBLE
                    }
                }
                withContext(Dispatchers.IO) {
                    if (needToAddLocationAtInitial) if (lastCurrentLocation != null) {
                        currentList.add(
                            0, Point.fromLngLat(
                                lastCurrentLocation!!.longitude, lastCurrentLocation!!.latitude
                            )
                        )
                    }

                    destination = currentList.last()

                    findRoute(onSuccessfullDraw)
                }
            } else {
                withContext(Dispatchers.Main) {
                    logger.logSelectContent("Route", "Complete", "Route demonstration completed")
                    offRouteButton.visibility = View.GONE
                    nextRouteButton.visibility = View.GONE
                    stopListenerThing()
//            onDone()
                }
            }
        }
    }


    fun clearRoute() {
        mapboxNavigation.stopTripSession()
        mapboxNavigation.setNavigationRoutes(emptyList())
    }

    // Example usage
    private fun filterPoints(originalList: List<LatLng>): List<Point> {
        val filteredList = mutableListOf<Point>()
        val distanceThreshold = 20.0

        originalList.forEachIndexed { index, latLng ->
            // Skip the first point
            if (index == 0 || index == originalList.size - 1) {
                filteredList.add(Point.fromLngLat(latLng.longitude, latLng.latitude))
            } else {
                val previousLatLng = originalList[index - 1]
                val distance = haversine(
                    previousLatLng.latitude,
                    previousLatLng.longitude,
                    latLng.latitude,
                    latLng.longitude
                )

                if (distance >= distanceThreshold) {
                    filteredList.add(Point.fromLngLat(latLng.longitude, latLng.latitude))
                }
            }
        }

        return filteredList
    }

    private fun unregisterObservers() {
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation.unregisterOffRouteObserver(offRouteProgressObserver)
        mapboxNavigation.unregisterArrivalObserver(arrivalObserver)
        mapboxNavigation.unregisterLocationObserver(locationObserver)

    }

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {}
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {

            if (locationMatcherResult.enhancedLocation.speed > 0f) {
                if (locationMatcherResult.enhancedLocation.bearing > 0) {
                    lastSpeed = locationMatcherResult.enhancedLocation.speed
                }
            }


            val enhancedLocation = locationMatcherResult.enhancedLocation
            lastCurrentLocation =
                LatLng(enhancedLocation.latitude, enhancedLocation.longitude)

            navigationLocationProvider.changePosition(
                enhancedLocation)
            updateCamera(
                Point.fromLngLat(
                    enhancedLocation.longitude, enhancedLocation.latitude
                ),
                enhancedLocation.bearing.toDouble()
            )
        }
    }


    private fun updateCamera(point: Point, bearing: Double?) {
//        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()

        /*val animationOptions = MapAnimationOptions.Builder()
            .duration(600)
            .animatorListener(object : Animator.AnimatorListener{
                override fun onAnimationStart(p0: Animator) {

                }

                override fun onAnimationEnd(p0: Animator) {
                }

                override fun onAnimationCancel(p0: Animator) {
                }

                override fun onAnimationRepeat(p0: Animator) {
                }
            }) */// listener updates isAnimating flag


//
//        if (System.currentTimeMillis() - mapCameraRecenterTimer > MAPBOX_DELAY_TIMER) {
//            mapCameraRecenterTimer = System.currentTimeMillis()
//            if (isFirstTime) {
//                isFirstTime = false
//                viewportDataSource.followingZoomPropertyOverride(17.0)
//                viewportDataSource.followingPadding =
//                    EdgeInsets(0.0, 0.0, ImageUtil.dpToPx(100).toDouble(), 0.0)
//            }
//        }
//        navigationCamera.requestNavigationCameraToFollowing()
//
        val location = Location("")
        location.latitude = point.latitude()
        location.longitude= point.longitude()
        location.bearing = bearing!!.toFloat()
        location.speed = lastSpeed
//        viewportDataSource.onLocationChanged(location)
//        viewportDataSource.evaluate()

        navigationCamera.requestNavigationCameraToFollowing()
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.evaluate()

//        mapView.camera.easeTo(
//            CameraOptions.Builder()
//                .center(point)
//                .bearing(bearing)
//                .pitch(45.0)
//                .zoom(17.0)
////                .padding( EdgeInsets(0.0, 0.0, ImageUtil.dpToPx(100).toDouble(), 0.0))
//                .build(),
////            animationOptions.build()
//        )
    }

    fun registerLocationObserver() {
        ApplicationStateData.getInstance().registerLocationObserver(this)
    }

    private fun registerObserver() {
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation.registerOffRouteObserver(offRouteProgressObserver)
        mapboxNavigation.registerArrivalObserver(arrivalObserver)
        mapboxNavigation.registerBannerInstructionsObserver { bannerInstructions -> }
        mapboxNavigation.registerLocationObserver(locationObserver)

    }

    private fun initCamera() {
        viewportDataSource = MapboxNavigationViewportDataSource(mapView.getMapboxMap())

        ApplicationStateData.getInstance().getCurrentLocation()
            .let { viewportDataSource.onLocationChanged(it) }
        navigationCamera = NavigationCamera(
            mapView.getMapboxMap(), mapView.camera, viewportDataSource
        )
        viewportDataSource.evaluate()
    }

    private fun initMapboxNavigation() {
        // Use LocationEngine to obtain the best location provider
        val locationEngine: LocationEngine = LocationEngineProvider.getBestLocationEngine(
            ApplicationStateData.getInstance().applicationContext
        )

        val downloadDirectory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath

        // Build NavigationOptions with the access token and location engine
        val navigationOptions = NavigationOptions.Builder(
            ApplicationStateData.getInstance().applicationContext
        ).accessToken(MAPBOX_ACCESS_TOKEN).historyRecorderOptions(
            HistoryRecorderOptions.Builder()
                .fileDirectory(downloadDirectory)
                .build()
        ).locationEngine(locationEngine) // Set the location engine
            .build()

        // Initialize MapboxNavigation with NavigationOptions
        mapboxNavigation = MapboxNavigationProvider.create(navigationOptions)
        mapboxNavigation.historyRecorder.startRecording()

    }

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    fun getLastLocationWithMapMatching(locationEngine: LocationEngine) {
        try {
            locationEngine.getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
                override fun onSuccess(result: LocationEngineResult?) {
                    val location = result?.lastLocation
                    if (location != null) {
                        // Convert the raw location to a Point
                        val rawPoint = Point.fromLngLat(location.longitude, location.latitude)

                        // Map-match the location and update the camera
//                        mapMatchLocationAndUpdateCamera(rawPoint)
                    }
                }

                override fun onFailure(exception: Exception) {
                    Log.e("LocationEngine", "Failed to get last location: ${exception.message}")
                }
            })
        } catch (e: SecurityException) {
            Log.e("LocationPermission", "Location permission not granted: ${e.message}")
        }
    }

    private fun mapMatchLocationAndUpdateCamera(rawPoint: Point, callback: (Point?) -> Unit) {
        if (previousPoint == null) {
            previousPoint = rawPoint
            callback(rawPoint) // Return the initial point as the matched point
            return
        }

        val mapboxMapMatching = MapboxMapMatching.builder()
            .accessToken(MAPBOX_ACCESS_TOKEN)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .coordinates(listOf(previousPoint!!, rawPoint))
            .build()

        mapboxMapMatching.enqueueCall(object : Callback<MapMatchingResponse> {
            override fun onResponse(
                call: Call<MapMatchingResponse>,
                response: Response<MapMatchingResponse>
            ) {
                val geometry = response.body()?.matchings()?.firstOrNull()?.geometry()
                if (geometry != null) {
                    val decodedPoints = PolylineUtils.decode(geometry, 6)
                    val mapMatchedPoint = decodedPoints.firstOrNull()
                    logger.logSelectContent(
                        "MapMatching",
                        "MapMatching",
                        gson.toJson(mapMatchedPoint)
                    )
                    callback(mapMatchedPoint) // Pass the map-matched point to the callback

//                    if (mapMatchedPoint != null) {
//                        mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(mapMatchedPoint).zoom(15.0).build())
//                    }
                } else {
                    callback(null) // No matched point found
                }
                previousPoint = rawPoint // Update the previous point
            }

            override fun onFailure(call: Call<MapMatchingResponse>, t: Throwable) {
                logger.logSelectContent("MapMatchingError","MapMatching", "Map matching failed: ${t.message}")
                callback(null) // Return null on failure
            }
        })
    }

    private fun initPuckLocation() {
        mapView.location.apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    ApplicationStateData.getInstance().applicationContext,
                    com.mapbox.navigation.R.drawable.mapbox_navigation_puck_icon
                )
            )
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }


        mapView.getMapboxMap().addOnMoveListener(object : OnMoveListener {
            override fun onMove(detector: MoveGestureDetector): Boolean {
                onCameraIdleListener?.onCameraIdle(null)
                return false
            }

            override fun onMoveBegin(detector: MoveGestureDetector) {
                onCameraIdleListener?.onCameraIdle(null)
            }

            override fun onMoveEnd(detector: MoveGestureDetector) {
                val option = mapView.getMapboxMap().cameraState.center
                if (option.latitude() > 0.0 && option.longitude() > 0.0) {
                    onCameraIdleListener?.let {
                        it.onCameraIdle(Location("").also {
                            it.latitude = option.latitude()
                            it.longitude = option.longitude()
                        })
                    }
                } else {
                    onCameraIdleListener?.onCameraIdle(null)
                }
            }
        })

    }

    private fun findRoute(onSuccessfullDraw: () -> Unit) {

        if (navigationRouteId != null) {
            mapboxNavigation.cancelRouteRequest(navigationRouteId!!)
        }

        calculationScope.launch(Dispatchers.Main) {
            startListenerThing()
        }

        routeScope.launch {

            logger.logSelectContent("Route", "Find", "Requesting route with waypoints")

            navigationRouteId =
                mapboxNavigation.requestRoutes(RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .applyLanguageAndVoiceUnitOptions(context)
                    .coordinatesList(currentList)
                    .waypointIndicesList(listOf(0, currentList.size - 1))
                    .profile(DirectionsCriteria.PROFILE_DRIVING).bannerInstructions(true)
                    .annotationsList(
                        listOf(
                            DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC,
                            DirectionsCriteria.ANNOTATION_DISTANCE,
                        )
                    ).geometries(DirectionsCriteria.GEOMETRY_POLYLINE6).voiceInstructions(true)
                    .steps(true).alternatives(false).enableRefresh(false).build(),
                    object : NavigationRouterCallback {

                        override fun onRoutesReady(
                            routes: List<NavigationRoute>, routerOrigin: RouterOrigin
                        ) {
                            routeScope.launch {

                                val newRoutes = if (isLastRound()) {
                                    routes
                                } else {
                                    Utils.updateContent(routes, logger)
                                }

                                Log.d(
                                    "ASD",
                                    "Current Zoom Level: ${mapView.getMapboxMap().cameraState.zoom}"
                                )
//
                                routeScope.launch(Dispatchers.Main) {

                                    if (lastCurrentLocation != null && !isRoutingCreatingFirstTime) {
                                        isRoutingCreatingFirstTime = true
                                        val location = Location(null)
                                        location.latitude = lastCurrentLocation!!.latitude
                                        location.longitude = lastCurrentLocation!!.longitude
                                        location.speed = 1f
                                        location.bearing = 0.1f
                                        onNewLocation(location)
                                    }


                                    setRouteAndStartNavigation(listOf(newRoutes.first()))
                                    renderRouteOnMap(newRoutes.first())
                                    isNavigationInProgress = true
                                    onSuccessfullDraw()
                                }
                            }
                        }

                        override fun onFailure(
                            reasons: List<RouterFailure>, routeOptions: RouteOptions
                        ) {
                            routeScope.launch(Dispatchers.Main) {
                                logger.logSelectContent(
                                    "requestRoutes", "onFailure", Gson().toJson(reasons)
                                )
                            }
                        }

                        override fun onCanceled(
                            routeOptions: RouteOptions, routerOrigin: RouterOrigin
                        ) { // no impl
                            routeScope.launch(Dispatchers.Main) {
                                logger.logSelectContent(
                                    "requestRoutes",
                                    "onCanceled",
                                    "Route cancelled"
                                )
                            }
                        }
                    })

        }
    }

    private fun isLastRound(): Boolean {
        return currentIndex == listOfChunks.size - 1
    }

    @SuppressLint("MissingPermission")
    private fun setRouteAndStartNavigation(routes: List<NavigationRoute>) { // set routes, where the first route in the list is the primary route that
        mapboxNavigation.setNavigationRoutes(routes)
        navigationCamera.requestNavigationCameraToFollowing()
        mapboxNavigation.startTripSession(false)
    }

    private fun extractRouteCoordinates(route: NavigationRoute) {
        val coordinatesList = mutableListOf<Point>()
        currentPathDrawingList.clear()
        // Iterate through each leg of the route
        route.directionsRoute.legs()?.forEach { leg ->
            // Iterate through each step of the leg
            leg.steps()?.forEach { step ->
                // Add the maneuver location (starting point of the step)
                val maneuverPoint = step.maneuver().location()
                coordinatesList.add(
                    Point.fromLngLat(
                        maneuverPoint.longitude(),
                        maneuverPoint.latitude()
                    )
                )

                // Add the intersections' locations (the points along the step)
                step.intersections()?.forEach { intersection ->
                    val intersectionPoint = intersection.location()
                    coordinatesList.add(
                        Point.fromLngLat(
                            intersectionPoint.longitude(),
                            intersectionPoint.latitude()
                        )
                    )
                }
            }
        }

        currentPathDrawingList.addAll(coordinatesList)
    }

    private fun renderRouteOnMap(route: NavigationRoute) {
        val routeLines = listOf(NavigationRouteLine(route, null))
        routeLineApi.setNavigationRouteLines(routeLines) { value ->
            mapView.getMapboxMap().getStyle()?.apply {
                routeLineView.renderRouteDrawData(this, value)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun onStart() {

        if (::mapboxNavigation.isInitialized && mapboxNavigation.isDestroyed.not()) {
            drawFirstTime = true
            startListenerThing()
        } else {

        }
    }

    fun startListenerThing() {
        registerLocationObserver()
        registerObserver()

    }

    fun stopListenerThing() {

        unregisterObservers()

        if (::voiceInstructionsPlayer.isInitialized) {
            voiceInstructionsPlayer.clear()
            voiceInstructionsPlayer.shutdown()
        }
        if (::speechApi.isInitialized) {
            speechApi.cancel()
        }

        if (mapboxNavigation != null && !mapboxNavigation.isDestroyed) {
            mapboxNavigation.stopTripSession()
            mapboxNavigation.resetTripSession(object : TripSessionResetCallback {
                override fun onTripSessionReset() {

                }
            })


            clearRoutesAndArrow()

        }
    }

    fun onStop() {
        logger.logSelectContent("Lifecycle", "onStop", "onStop called")
        if (::mapboxNavigation.isInitialized || mapboxNavigation.isDestroyed.not()) {
            mapboxNavigation.resetTripSession()
            mapboxNavigation.setNavigationRoutes(emptyList())
            unregisterObservers()
            ApplicationStateData.getInstance().registerLocationObserver(null)
            speechApi.cancel()
            voiceInstructionsPlayer.shutdown()
            clearRoutesAndArrow()
            logger.logSelectContent(
                "Navigation", "onStop", "Navigation session stopped and listeners unregistered"
            )
        }
    }

    fun onResume() {
        logger.logSelectContent("Lifecycle", "onResume", "onResume called")
        if (::mapboxNavigation.isInitialized || mapboxNavigation.isDestroyed.not()) {

            onStart()
            if (isNavigationInProgress == true) {
                drawRouteAgain()
                logger.logSelectContent("Route", "drawRouteAgain", "Route redrawn")
            }
        }
    }

    companion object {
        private val TAG = MapApplication::class.java.simpleName
    }

    private fun formatTime(seconds: Double): String {
        if (seconds < 60) {
            return String.format("%1$.0f sec", seconds)
        }
        if (seconds < (60 * 2)) // less than 2 minutes
        {
            return String.format("%1$.0f min<br>%2$.0f sec", seconds / 60, seconds % 60)
        }

        return if (seconds < (60 * 60)) // less than 1 hour
        {
            String.format("%1$.0f min", seconds / 60, seconds % 60)
        } else {
            String.format(
                "%1$.0f hr<br>%2$.0f min", seconds / (60 * 60), (seconds % (60 * 60)) / 60
            )
        }
    }

    private fun formatDistance(meters: Double): String {
        return if (meters < 1000) {
            String.format("%.0f m", meters);
        } else if (meters < 10000) {
            String.format("%.1f km", meters / 1000.0);
        } else {
            String.format("%.0f km", meters / 1000.0);
        }
    }

    override fun onNewLocation(location: Location?) {
        location?.let {
            if (it.speed > 0f) {
                if (it.bearing > 0) {
                    lastSpeed = location.speed

                    val rawPoint = Point.fromLngLat(it.longitude, it.latitude)
                    mapMatchLocationAndUpdateCamera(rawPoint) { mapMatchedPoint ->

                        if (mapMatchedPoint != null) {
                            val mapMatchedLocation = Location(it).apply {
                                latitude = mapMatchedPoint.latitude()
                                longitude = mapMatchedPoint.longitude()
                            }
                            lastCurrentLocation =
                                LatLng(mapMatchedLocation.latitude, mapMatchedLocation.longitude)
                            navigationLocationProvider.changePosition(
                                location = mapMatchedLocation,
                                keyPoints = emptyList(),
                            )
                        } else {
                            // Fall back to raw location if map-matching fails
                            lastCurrentLocation = LatLng(it.latitude, it.longitude)
                            navigationLocationProvider.changePosition(
                                location = it,
                                keyPoints = emptyList(),
                            )
                        }
                    }
//                    navigationLocationProvider.changePosition(
//                        location = it,
//                        keyPoints = emptyList(),
//                    )

                    if (System.currentTimeMillis() - mapCameraRecenterTimer > MAPBOX_DELAY_TIMER) {
                        mapCameraRecenterTimer = System.currentTimeMillis()
                        if (isFirstTime) {
                            isFirstTime = false
                            viewportDataSource.followingZoomPropertyOverride(17.0)
                            viewportDataSource.followingPadding =
                                EdgeInsets(0.0, 0.0, ImageUtil.dpToPx(100).toDouble(), 0.0)
                        }
                    }
                    navigationCamera.requestNavigationCameraToFollowing()
                    viewportDataSource.onLocationChanged(it)
                    viewportDataSource.evaluate()
                } else {
//                    Log.e("Location", Gson().toJson(location))
                }
            }
        }
    }

    interface OnCameraIdleListener {
        fun onCameraIdle(location: Location?)
    }

    fun drawRouteAgain() {
        findRoute({})
    }

    fun calculateOffRouting(isOffRouted: Boolean = false) {
        isOffRoute = isOffRouted

        calculationScope.launch(Dispatchers.Main) {
            logger.logSelectContent("Navigation", "OffRoute", "Off-route calculation started")
            offRouteButton.visibility = View.GONE

        }
        calculationScope.launch {
            if (listOfChunks != null && listOfChunks.size > 0) {

                if (!isLastRound()) {
                    var (index, nestedIndex) = findNearestChunk(
                        listOfChunks,
                        lastCurrentLocation!!
                    )

                    index = if (index == -1) (listOfChunks.size - 1) else index
                    nestedIndex = if (nestedIndex == -1) 0 else nestedIndex

                    if (nestedIndex != (listOfChunks[index].list.size - 1) && (nestedIndex + 1) != (listOfChunks[index].list.size - 1)) {
                        nestedIndex = nestedIndex + 1
                    }

                    val list = ArrayList<Point>()

                    if (isOffRoutingFirstTime && index == 0) {
                        listOfChunks.get(0).list.removeAt(0)
//                    nestedIndex = nestedIndex - 1
//                    if (nestedIndex == -1) {
//                        nestedIndex = 0
//                    }
                    }

                    list.add(lastCurrentLocation!!.getPoint())
                    list.addAll(
                        listOfChunks.get(index).list.subList(
                            nestedIndex, listOfChunks.get(index).list.size
                        )
                    )
                    isOffRoutingFirstTime = true
//                val startingPoint  = ChunkModel(false, firstList, linearDistanceInMeter = calculateTotalHaversineDistance(firstList));

                    Log.e("FILTER", "FOUND INDEX: " + index)
                    Log.e("FILTER", "FOUND NESTED INDEX: " + nestedIndex)

                    if (index != (listOfChunks.get(index).list.size - 1)) {
                        Log.e("FILTER", "BEFORE ADD SIZE: " + list.size)
                        for (i in (index + 1..listOfChunks.size - 1)) {
                            list.addAll(listOfChunks.get(i).list)
                        }
                        Log.e("FILTER", "BEFORE ADD SIZE: " + list.size)
                    }


                    withContext(Dispatchers.Main) {
                        startLocationTracking(list)
                    }
                } else {

                    val list = ArrayList<Point>()
                    list.add(
                        Point.fromLngLat(
                            lastCurrentLocation!!.longitude, lastCurrentLocation!!.latitude
                        )
                    )
                    list.add(currentList.last())
                    if (!isAddedTheLastPointIndex) {
                        isAddedTheLastPointIndex = true
                        listOfChunks.add(ChunkModel(false, list, 0.0))
                    } else {
                        listOfChunks.get(currentIndex).list.clear()
                        listOfChunks.get(currentIndex).list = list
                        currentIndex = currentIndex - 1
                    }

                    withContext(Dispatchers.Main) {
                        startNextRoute(false)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    logger.logSelectContent(
                        "Navigation", "OffRouteError", "No data found in chunks for off-route"
                    )
                    Log.e("mapApplication", "No Data found")
                }
            }
        }
    }

    fun startNextRoute(needToAddLocationAtInitial: Boolean) {
        Log.e("mapApplication", "Calling from startNextRoute()")
        stopListenerThing()
        isNavigationInProgress = false
        listOfChunks.get(currentIndex).isVisited = true
        updateListRoute(needToAddLocationAtInitial, {
            drawFirstTime = true
        })
    }

    suspend fun findNearestChunk(
        listOfChunks: List<ChunkModel>, lastCurrentLocation: LatLng
    ): Pair<Int, Int> {
        return withContext(Dispatchers.IO) {
            var shortestDistance = Double.MAX_VALUE
            var index: Int = -1
            var nestedIndex: Int = -1

            for (chunkIndex in listOfChunks.indices) {
                if (!listOfChunks[chunkIndex].isVisited) {
                    val chunk = listOfChunks[chunkIndex]
                    for (pointIndex in chunk.list.indices) {
                        val point = chunk.list[pointIndex]

                        val distance = haversine(
                            lastCurrentLocation.latitude,
                            lastCurrentLocation.longitude,
                            point.latitude(),
                            point.longitude()
                        )

                        if (distance < shortestDistance) {
                            shortestDistance = distance
                            nestedIndex = pointIndex
                            index = chunkIndex
                        }
                    }
                }
            }

            if (index > 0) {
                Log.e("FILTER", "Index: " + index.toString())
                for (j in 0..(index - 1)) {
                    Log.e(
                        "FILTER",
                        "${j} = isVisited: " + listOfChunks.get(j).isVisited.toString()
                    )
                    if (!listOfChunks.get(j).isVisited) {
                        listOfChunks.get(j).isVisited = true
                    }
                }
            }

            logger.logSelectContent(
                "Navigation",
                "findNearestChunk",
                "Nearest chunk found at index: $index and nestedIndex: $nestedIndex"
            )

            Pair(index, nestedIndex)
        }
    }

    private fun addMarker(point: Point) {
        val pointAnnotationManager: PointAnnotationManager =
            mapView.annotations.createPointAnnotationManager()

        // Set marker position (latitude and longitude)
        // Create a marker annotation
        val pointAnnotationOptions = PointAnnotationOptions().withPoint(point)
            .withIconImage(getBitmapFromMipmap(R.mipmap.location))

        pointAnnotationManager.create(pointAnnotationOptions)
    }

    private fun getBitmapFromMipmap(resourceId: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, resourceId)
    }

    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a =
            Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(
                Math.toRadians(lat2)
            ) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    fun haversineInMeter(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0  // Radius of the Earth in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a =
            sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(
                dLon / 2
            ) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = R * c
        return "%.3f".format(distance).toDouble()

    }

    // Function to calculate the total Haversine distance for a list of lists of Points
    fun calculateTotalHaversineDistance(listOfCoordinates: List<Point>): Double {
        var totalDistance = 0.0

        for (i in 0 until listOfCoordinates.size - 1) {
            val point1 = listOfCoordinates[i]
            val point2 = listOfCoordinates[i + 1]
            totalDistance += haversineInMeter(
                point1.latitude(), point1.longitude(), point2.latitude(), point2.longitude()
            )
        }

        return totalDistance
    }

    // Function to calculate travel time based on speed (in km/h)
    fun calculateTravelTime(distance: Double, speed: Double): Double {
        // Set a minimum speed threshold (e.g., 10 km/h) to avoid drastic ETA changes at signals
        val minimumSpeed = 30.0
        val adjustedSpeed = if (speed > minimumSpeed) speed else minimumSpeed

        return distance / adjustedSpeed  // Time in hours
    }

//    // Function to calculate travel time based on speed (in km/h)
//    fun calculateTravelTime(distance: Double, speed: Double): Double {
//        val minimumSpeed = 10.0
//        val adjustedSpeed = if (speed > minimumSpeed) speed else minimumSpeed
//        if (speed > 0) {
//            return distance / speed  // Time in hours
//        } else {
//            return distance / 30
//        }
//    }

    fun increaseOffRouteCount() {
//        offRouteCount = offRouteCount + 1
//        if (offRouteCount > 1) {
        if (wazeButton.visibility == View.GONE)
            wazeButton.visibility = View.VISIBLE
//        }
    }

    fun hideWazeButton() {
        if (currentIndex < listOfChunks.size - 1) {
//            wazeButton.visibility = View.GONE
        }
    }
}
package com.karwa.mdtnavigation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.maps.android.SphericalUtil
import com.karwa.mdtnavigation.model.ChunkModel
import com.karwa.mdtnavigation.model.getDistance
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.StyleContract
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.easeTo
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
    var nextRouteButton: Button
) : KLocationObserver {
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
    private var lastDestinationCurrentPoint: Point? = null
    private var lastDestinationOverallPoint: Point? = null
    private lateinit var context: Context

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

    /**
     * Gets notified with progress along the currently active route.
     */
    private val routeProgressObserver: RouteProgressObserver =
        RouteProgressObserver { routeProgress -> // update the camera position to account for the progressed fragment of the route
//            Log.d("MapApplication", "---------------")
            val fractionTraveled = routeProgress.fractionTraveled
//            Log.d("MapApplication", "Fraction Traveled: $fractionTraveled")

            val traveledDistance = routeProgress.distanceTraveled
            val remainingDistance = routeProgress.distanceRemaining
            // Update the map with the current progress (arrows, polylines, etc.)
//            updateProgressOnTheMap(routeProgress)

//            val remainingDistance = routeProgress.distanceRemaining
//            val remainingTime = routeProgress.durationRemaining

//            Log.d("MapApplication", "Remaining Distance: $remainingDistance meters")
//            Log.d("MapApplication", "Remaining Time: $remainingTime seconds")
//            Log.d("MapApplication", "Route Progress State: ${routeProgress.currentState}")
//
            updateProgressOnTheMap(routeProgress)
            val arrival = Calendar.getInstance()
            arrival.add(Calendar.SECOND, routeProgress.durationRemaining.toInt())

//            if (routeProgress.currentState == RouteProgressState.COMPLETE || remainingDistance <= 10) {
//                Log.d("MapApplication", "Route is complete or vehicle is near the destination.")
//                onUserApproachingDestination()
//            }

            updateApplicationState(routeProgress, arrival)
//            updateRouteLine(traveledDistance, remainingDistance, routeProgress.navigationRoute)

            if (lastCurrentLocation != null) {
                val distanceInMeters = SphericalUtil.computeDistanceBetween(
                    LatLng(
                        lastCurrentLocation!!.latitude, lastCurrentLocation!!.longitude
                    ), LatLng(destination!!.latitude(), destination!!.longitude())
                )
//                Log.d(
//                    "MapApplication",
//                    "Current LatLng: ${lastCurrentLocation!!.latitude}, ${lastCurrentLocation!!.longitude}"
//                )
                Log.d("MapApplication", "Distance:: ${distanceInMeters}")


                if (distanceInMeters < 150.0) {
                    nextRouteButton.visibility = View.VISIBLE
                } else {
                    nextRouteButton.visibility = View.GONE
                }
                if (fractionTraveled >= 0.80) {
                    if (isLastRound()) {
                        if (fractionTraveled >= 0.96) {
                            startNextRoute(true)
                        }
                    } else {
//                        Log.d(
//                            "MapApplication",
//                            "User is almost approaching to the section destination"
//                        )
                        startNextRoute(true)
                    }
                }
            }

        }

    private fun updateApplicationState(routeProgress: RouteProgress, arrival: Calendar) {
        if (routeProgress.currentState == RouteProgressState.COMPLETE || routeProgress.fractionTraveled >= 1.0) {
            ApplicationStateData.getInstance().txtArrivalTime = ("--:--")
            ApplicationStateData.getInstance().arrivalTime = (0)
            ApplicationStateData.getInstance().txtRemainingDistance = ("--")
            ApplicationStateData.getInstance().txtRemainingTime = ("--")
            ApplicationStateData.getInstance().setEtaToStop(0.0)

        } else {
            if (isLastRound()) {
                ApplicationStateData.getInstance().txtRemainingDistance =
                    (formatDistance(routeProgress.distanceRemaining.toDouble()))

                ApplicationStateData.getInstance().txtRemainingTime =
                    (formatTime(routeProgress.durationRemaining))

                ApplicationStateData.getInstance().setEtaToStop(routeProgress.durationRemaining)
                ApplicationStateData.getInstance().arrivalTime = (arrival.timeInMillis)
                ApplicationStateData.getInstance().txtArrivalTime = (String.format(
                    "%1$02d:%2$02d", arrival.get(Calendar.HOUR_OF_DAY), arrival.get(Calendar.MINUTE)
                ))
            } else {
                var chunkDistance = listOfChunks.getDistance()
//                var newDistance =
//                    routeProgress.distanceTraveled + routeProgress.distanceRemaining + chunkDistance
                Log.e("distance", "Travel Distance:::        " + routeProgress.distanceTraveled)
                Log.e("distance", "Remaining Distance:::     " + routeProgress.distanceRemaining)
                ApplicationStateData.getInstance().txtRemainingDistance =
                    (formatDistance(routeProgress.distanceRemaining.toDouble() + chunkDistance))
//                Log.e("distance", "new remaining Distance::: " + newDistance)
//                ApplicationStateData.getInstance().txtRemainingDistance =
//                    (formatDistance(routeProgress.distanceRemaining.toDouble() + chunkDistance)) + "/" + (formatDistance(
//                        newDistance
//                    ))

                ApplicationStateData.getInstance().txtRemainingTime =
                    (formatTime(routeProgress.durationRemaining))

                ApplicationStateData.getInstance().setEtaToStop(routeProgress.durationRemaining)
                ApplicationStateData.getInstance().arrivalTime = (arrival.timeInMillis)
                ApplicationStateData.getInstance().txtArrivalTime = (String.format(
                    "%1$02d:%2$02d", arrival.get(Calendar.HOUR_OF_DAY), arrival.get(Calendar.MINUTE)
                ))
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
            maneuverView?.visibility = View.VISIBLE
            maneuverView!!.renderManeuvers(maneuvers)
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

    private val offRouteProgressObserver: OffRouteObserver = OffRouteObserver { isOffRoute ->
        if (isOffRoute) {
//            Log.d("MapApplication", "Vehicle went off-route, recalculating route.")
            // You can either force recalculation or check the remaining distance manually
            offRouteButton.visibility = View.VISIBLE
        } else {
//            Log.d("MapApplication", "Vehicle none off-route")
        }
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
            destination?.let { navigateToFixedRoute(it, encodedPath, context) }
            isNavigationInProgress = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

            style.addLayerBelow(lineLayer, "mapbox-masking-layer-main")
        }

    }

    @SuppressLint("MissingPermission")
    fun navigateToFixedRoute(destination: Point, encodedPath: String?, context: Context) {
        this.context = context
//        val originLocation = ApplicationStateData.getInstance().getCurrentLocation()
//        val originPoint = Point.fromLngLat(
//            originLocation!!.longitude, originLocation!!.latitude
//        )

        stopListenerThing()

        val list = DummyContent.listOfTTestLatLng()
//        val list = PolyUtil.decode(encodedPath)

//        Log.e("MapApplication", Gson().toJson(list.size.toString()))
//        Log.e("MapApplication", Gson().toJson(list.first()))
//        Log.e("MapApplication", Gson().toJson(list.last()))

        val finalList = mutableListOf<Point>()
        for (i in 0 until list.size - 1) {
            finalList.add(Point.fromLngLat(list[i].longitude, list[i].latitude))
        }

        lastDestinationOverallPoint = finalList.last()
        drawSimplePolyline(finalList)
        calculateTimeAndDistance(finalList)

        listOfChunks = ArrayList(finalList.chunked(15).map {
            var distance = calculateTotalHaversineDistance(it)
            Log.e("distance", "Distance::: " + distance)
            ChunkModel(list = ArrayList(it), linearDistanceInMeter = distance)
        })

        addMarker(finalList.last())

        Log.e("mapApplication", "Calling from navigateToFixedRoute()")
        updateListRoute(false, {})
    }

    private fun updateListRoute(
        needToAddLocationAtInitial: Boolean = true,
        onSuccessfullDraw: () -> Unit
    ) {
        if ((currentIndex + 1) < listOfChunks.size) {

            currentIndex = currentIndex + 1
            lastDestinationCurrentPoint = listOfChunks.get(currentIndex).list.last()

            currentList = listOfChunks.get(currentIndex).list
            if (needToAddLocationAtInitial)
                if (lastCurrentLocation != null) {
                    currentList.add(
                        0, Point.fromLngLat(
                            lastCurrentLocation!!.longitude, lastCurrentLocation!!.latitude
                        )
                    )
                }

            destination = currentList.last()

            findRoute(onSuccessfullDraw)
        } else {
            Toast.makeText(context, "Trip demostration Completed", Toast.LENGTH_SHORT).show()
//            Log.e("MapApplication", "NO Route Found")
            stopListenerThing()
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

        // Build NavigationOptions with the access token and location engine
        val navigationOptions = NavigationOptions.Builder(
            ApplicationStateData.getInstance().applicationContext
        ).accessToken(MAPBOX_ACCESS_TOKEN).locationEngine(locationEngine) // Set the location engine
            .build()

        // Initialize MapboxNavigation with NavigationOptions
        mapboxNavigation = MapboxNavigationProvider.create(navigationOptions)
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


    private fun filterClosePoints(points: List<Point>, distanceThreshold: Double): List<Point> {
        val filteredPoints = mutableListOf<Point>()
        points.forEachIndexed { index, point ->
            if (index == 0 || index == points.size - 1) {
                filteredPoints.add(point)
            } else {
                val previousPoint = points[index - 1]
                val distance = haversine(
                    previousPoint.latitude(),
                    previousPoint.longitude(),
                    point.latitude(),
                    point.longitude()
                )
                if (distance >= distanceThreshold) {
                    filteredPoints.add(point)
                }
            }
        }
        return filteredPoints
    }

    private fun findRoute(onSuccessfullDraw: () -> Unit) {

//        Log.d("MapApplication", "Current LatLng List: ${currentList.size}")
//        Log.d("MapApplication", "Current LatLng List: ${currentIndex}")

        if (navigationRouteId != null) {
            mapboxNavigation.cancelRouteRequest(navigationRouteId!!)
        }


        startListenerThing()

//        Log.e("MapApplication", (0 until currentList.size).toList().toString())
//        Log.e("MapApplication", "Size: " + (currentList.size).toString())
//        Log.e(
//            "MapApplication",
//            "Coordinate: " + currentList.last().latitude() + "," + currentList.last().longitude()
//        )
//        val list = currentList.subList(1, currentList.size - 1)

        navigationRouteId =
            mapboxNavigation.requestRoutes(RouteOptions.builder().applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(context)
//                .coordinates(origin = currentList.first(), destination = currentList.last())
                .coordinatesList(currentList)
                .waypointIndicesList(listOf(0, currentList.size - 1))
//                .waypointTargetsList(currentList)
//                .waypointIndicesList((0 until currentList.size).toMutableList())
                .profile(DirectionsCriteria.PROFILE_DRIVING).bannerInstructions(true)
                .annotationsList(
                    listOf(
                        DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC,
                        DirectionsCriteria.ANNOTATION_DISTANCE,
                    )
                ).geometries(DirectionsCriteria.GEOMETRY_POLYLINE).voiceInstructions(true)
                .steps(true).alternatives(false).enableRefresh(false).build(),
                object : NavigationRouterCallback {

                    override fun onRoutesReady(
                        routes: List<NavigationRoute>, routerOrigin: RouterOrigin
                    ) {
//                        Log.d(
//                            "Route Info",
//                            "Route geometry: ${routes.first().routeOptions.geometries()}"
//                        )
//                        Log.e("Instruction", "${currentIndex}==${listOfChunks.size - 1}")
                        val newRoutes = if (isLastRound()) {
                            routes
                        } else {
                            Utils.updateContent(routes)
                        }
//                    val gsopnData = Gson().toJson(routerOrigin)
                        val gsopnData1 = Gson().toJson(newRoutes)
//                        Log.e("Instruction", gsopnData1)

                        setRouteAndStartNavigation(listOf(newRoutes.first()))
                        renderRouteOnMap(newRoutes.first())
                        isNavigationInProgress = true
                        onSuccessfullDraw()
                    }

                    override fun onFailure(
                        reasons: List<RouterFailure>, routeOptions: RouteOptions
                    ) {
                        Log.e("MapApplication", "onFailure")
                    }

                    override fun onCanceled(
                        routeOptions: RouteOptions, routerOrigin: RouterOrigin
                    ) { // no impl
                        Log.e("MapApplication", "onCanceled")
                    }
                })

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
            //and recenter camera in Trip Session
            drawFirstTime = true
            startListenerThing()
            //processNavigation()
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

        mapboxNavigation.stopTripSession()
        mapboxNavigation.resetTripSession(object : TripSessionResetCallback {
            override fun onTripSessionReset() {

            }
        })

        clearRoutesAndArrow()

    }

    fun onStop() {
        if (::mapboxNavigation.isInitialized || mapboxNavigation.isDestroyed.not()) {
            mapboxNavigation.resetTripSession()
            mapboxNavigation.setNavigationRoutes(emptyList())
            unregisterObservers()
            ApplicationStateData.getInstance().registerLocationObserver(null)
            speechApi.cancel()
            voiceInstructionsPlayer.shutdown()
            clearRoutesAndArrow()
        }
    }

    fun onResume() {
        if (::mapboxNavigation.isInitialized || mapboxNavigation.isDestroyed.not()) {

            onStart()
            if (isNavigationInProgress == true) {
                drawRouteAgain()
            }
        }
    }

//    fun stopNavigation() {
//
//        unregisterObservers()
//        clearRoutesAndArrow()
//
//
//    }

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
            if (it.bearing > 0) {
                lastCurrentLocation = LatLng(it.latitude, it.longitude)
                navigationLocationProvider.changePosition(
                    location = it,
                    keyPoints = emptyList(),
                )

                if (System.currentTimeMillis() - mapCameraRecenterTimer > MAPBOX_DELAY_TIMER) {
                    mapCameraRecenterTimer = System.currentTimeMillis()

                    if (isFirstTime) {
                        isFirstTime = false
                        viewportDataSource.followingZoomPropertyOverride(17.0)
                        viewportDataSource.followingPadding =
                            EdgeInsets(0.0, 0.0, ImageUtil.dpToPx(250).toDouble(), 0.0)
                    }
                }
                navigationCamera.requestNavigationCameraToFollowing()
                viewportDataSource.onLocationChanged(it)
                viewportDataSource.evaluate()
            }
        }
    }

//    private fun onUserReachedDestination() {
//        stopNavigation()
//        Log.d("MapApplication", "User has arrived at the destination!")
//    }

    fun setCurrentLocation(location: Location?) {
        location?.let {
            mapView.getMapboxMap().easeTo(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(location.longitude, location.latitude)).zoom(17.0)
                    .padding(EdgeInsets(500.0, 0.0, 0.0, 0.0)).build()
            )


        }
    }


    fun registerCameraMoveLocationObserver(listener: OnCameraIdleListener) {
        onCameraIdleListener = listener
    }

    interface OnCameraIdleListener {
        fun onCameraIdle(location: Location?)
    }

    fun drawRouteAgain() {
        findRoute({})
    }

    fun calculateOffRouting() {
        isOffRoute = true
        //Need to calcualte off routing functionality
        offRouteButton.visibility = View.GONE

        if (listOfChunks != null && listOfChunks.size > 0) {

            if (!isLastRound()) {
//                var index = -1 //findNearestChunk(listOfChunks, lastCurrentLocation!!)
                var (index, nestedIndex) = findNearestChunk(listOfChunks, lastCurrentLocation!!)


                index = if (index == -1) (listOfChunks.size - 1) else index
                nestedIndex = if (nestedIndex == -1) 0 else nestedIndex

                val chunk = listOfChunks[index]

                // Calculate distances before and after the nearest point
                val distanceBefore = if (nestedIndex > 0) {
                    haversine(
                        lastCurrentLocation!!.latitude,
                        lastCurrentLocation!!.longitude,
                        chunk.list[nestedIndex - 1].latitude(),
                        chunk.list[nestedIndex - 1].longitude()
                    )
                } else {
                    Double.MAX_VALUE  // No point before, set to a high value
                }

                val distanceAfter = if (nestedIndex < chunk.list.size - 1) {
                    haversine(
                        lastCurrentLocation!!.latitude,
                        lastCurrentLocation!!.longitude,
                        chunk.list[nestedIndex + 1].latitude(),
                        chunk.list[nestedIndex + 1].longitude()
                    )
                } else {
                    Double.MAX_VALUE  // No point after, set to a high value
                }

//                val list = ArrayList<Point>()
//                list.add(
//                    Point.fromLngLat(
//                        lastCurrentLocation!!.longitude, lastCurrentLocation!!.latitude
//                    )
//                )
//                list.add(
//                    lastDestinationOverallPoint!!
//                )
//                listOfChunks.add(list)
//                index = list.size
                if (index != -1) {
                    currentIndex = if (index == 0) 0 else index - 1
                    // Insert based on which neighboring distance is smaller
                    if (currentList.size < 24) {
                        if (distanceBefore < distanceAfter) {
                            // Insert above the nearest point (before nestedIndex)
                            listOfChunks.get(currentIndex).list.add(
                                nestedIndex,
                                Point.fromLngLat(
                                    lastCurrentLocation!!.longitude,
                                    lastCurrentLocation!!.latitude
                                )
                            )
//                            Log.e(
//                                "mapApplication",
//                                "Nearest: Inserted at position above nearest index"
//                            )
                        } else {
                            // Insert below the nearest point (after nestedIndex)
                            listOfChunks.get(currentIndex + 1).list.add(
                                nestedIndex,
                                Point.fromLngLat(
                                    lastCurrentLocation!!.longitude,
                                    lastCurrentLocation!!.latitude
                                )
                            )
//                            Log.e(
//                                "mapApplication",
//                                "Nearest: Inserted at position below nearest index"
//                            )
                        }
                    }

//                    Log.e(
//                        "mapApplication",
//                        "Nearest Coordinates List: " + Gson().toJson(listOfChunks.get(index))
//                    )
                    startNextRoute(false)
//                    Log.e("mapApplication", "OffRoute: Nearest: " + index)
                } else {
                    Log.e("mapApplication", "location not found in any coordinates")
                }
            } else {
                //User at last round
            }
        } else {
            Log.e("mapApplication", "No Data found")
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

    fun findNearestChunk(
        listOfChunks: List<ChunkModel>, lastCurrentLocation: LatLng
    ): Pair<Int, Int> {
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
        return Pair(index, nestedIndex)
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
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(
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

        // Iterate over each list of points
//        for (coordinateList in listOfCoordinates) {
        for (i in 0 until listOfCoordinates.size - 1) {
            val point1 = listOfCoordinates[i]
            val point2 = listOfCoordinates[i + 1]
            totalDistance += haversineInMeter(
                point1.latitude(), point1.longitude(), point2.latitude(), point2.longitude()
            )
//            }
        }

        return totalDistance
    }

    // Function to calculate travel time based on speed (in km/h)
    fun calculateTravelTime(distance: Double, speed: Double): Double {
        return distance / speed  // Time in hours
    }

    private fun calculateTimeAndDistance(list: List<Point>) {
        val totalDistance = calculateTotalHaversineDistance(list)

// Adjusting travel time based on different speeds
        val walkingSpeed = 5.0  // km/h
        val cyclingSpeed = 15.0 // km/h
        val drivingSpeed = 30.0 // km/h

        val walkingTime = calculateTravelTime(totalDistance, walkingSpeed)
        val cyclingTime = calculateTravelTime(totalDistance, cyclingSpeed)
        val drivingTime = calculateTravelTime(totalDistance, drivingSpeed)

//        Log.e("mapApplication", "Total Distance: $totalDistance km")
//        Log.e("mapApplication", "Walking Time: $walkingTime hours")
//        Log.e("mapApplication", "Cycling Time: $cyclingTime hours")
//        Log.e("mapApplication", "Driving Time: $drivingTime hours")
    }

}
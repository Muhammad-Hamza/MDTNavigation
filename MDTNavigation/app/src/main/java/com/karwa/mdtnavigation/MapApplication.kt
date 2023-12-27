//package com.karwa.mdtnavigation
//
//import android.annotation.SuppressLint
//import android.location.Location
//import android.view.View
//import android.widget.Toast
//import android.widget.ToggleButton
//import androidx.core.content.ContextCompat
//import com.google.android.gms.maps.model.LatLng
//import com.google.maps.android.PolyUtil
//import com.karwatechnologies.mdt.R
//import com.karwatechnologies.mdt.activities.MasterActivity.isTripStarted
//import com.karwatechnologies.mdt.callbacks.KLocationObserver
//import com.karwatechnologies.mdt.data.Config.MAPBOX_ACCESS_TOKEN
//import com.karwatechnologies.mdt.data.ApplicationStateData
//import com.karwatechnologies.mdt.data.local.MDTLocation
//import com.karwatechnologies.mdt.data.remote.FixedRouteResponse
//import com.karwatechnologies.mdt.location.LocationUtil
//import com.karwatechnologies.mdt.utils.ImageUtil
//import com.karwatechnologies.mdt.utils.LogUtil
//import com.karwatechnologies.mdt.utils.dateTimeUtils.DateTimeUtil
//import com.karwatechnologies.mdt.utils.NetworkAvailabilityUtil
//import com.karwatechnologies.mdt.utils.convertTo
//import com.mapbox.android.core.location.LocationEngineProvider
//import com.mapbox.android.gestures.MoveGestureDetector
//import com.mapbox.api.directions.v5.DirectionsCriteria
//import com.mapbox.api.directions.v5.models.Bearing
//import com.mapbox.api.directions.v5.models.DirectionsRoute
//import com.mapbox.api.directions.v5.models.RouteOptions
//import com.mapbox.api.matching.v5.MapboxMapMatching
//import com.mapbox.api.matching.v5.models.MapMatchingResponse
//import com.mapbox.bindgen.Expected
//import com.mapbox.geojson.Point
//import com.mapbox.maps.CameraOptions
//import com.mapbox.maps.EdgeInsets
//import com.mapbox.maps.MapView
//import com.mapbox.maps.plugin.LocationPuck2D
//import com.mapbox.maps.plugin.animation.camera
//import com.mapbox.maps.plugin.animation.easeTo
//import com.mapbox.maps.plugin.gestures.OnMoveListener
//import com.mapbox.maps.plugin.gestures.addOnMoveListener
//import com.mapbox.maps.plugin.locationcomponent.location
//import com.mapbox.navigation.base.TimeFormat
//import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
//import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
//import com.mapbox.navigation.base.formatter.UnitType
//import com.mapbox.navigation.base.options.NavigationOptions
//import com.mapbox.navigation.base.route.*
//import com.mapbox.navigation.base.trip.model.RouteLegProgress
//import com.mapbox.navigation.base.trip.model.RouteProgress
//import com.mapbox.navigation.base.trip.model.RouteProgressState
//import com.mapbox.navigation.core.MapboxNavigation
//import com.mapbox.navigation.core.MapboxNavigationProvider
//import com.mapbox.navigation.core.arrival.ArrivalObserver
//import com.mapbox.navigation.core.directions.session.RoutesObserver
//import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
//import com.mapbox.navigation.core.replay.MapboxReplayer
//import com.mapbox.navigation.core.replay.ReplayLocationEngine
//import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
//import com.mapbox.navigation.core.trip.session.*
//import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
//import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
//import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
//import com.mapbox.navigation.ui.maps.camera.NavigationCamera
//import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
//import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
//import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
//import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
//import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
//import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
//import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
//import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
//import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
//import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
//import com.mapbox.navigation.ui.tripprogress.model.*
//import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
//import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
//import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
//import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
//import com.mapbox.navigation.ui.voice.model.SpeechError
//import com.mapbox.navigation.ui.voice.model.SpeechValue
//import com.mapbox.navigation.ui.voice.model.SpeechVolume
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import java.util.*
//
//
//class MapApplication constructor(var mapView: MapView,  var maneuverView: Mapb?,  var voiceToggleButton: ToggleButton):
//    KLocationObserver
//{
//    private var destination: Point? = null
//
//    var isNavigationInProgress = false
//    var navigationRouteId: Long? = null
//    private var isFirstTime = true
//    private var mapCameraRecenterTimer = 0L
//    private val MAPBOX_DELAY_TIMER: Long = 5000
//    private var lastOffrouteTime: Long = 0
//    private val OFF_ROUTE_RETRY: Long = 5000
//    /**
//     * [NavigationLocationProvider] is a utility class that helps to provide location updates generated by the Navigation SDK
//     * to the Maps SDK in order to update the user location indicator on the map.
//     */
//    private val navigationLocationProvider = NavigationLocationProvider()
//
//    /**
//     * Mapbox Navigation entry point. There should only be one instance of this object for the app.
//     * You can use [MapboxNavigationProvider] to help create and obtain that instance.
//     */
//    private lateinit var mapboxNavigation: MapboxNavigation
//
//    /**
//     * Produces the camera frames based on the location and routing data for the [navigationCamera] to execute.
//     */
//    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
//
//    /**
//     * Used to execute camera transitions based on the data generated by the [viewportDataSource].
//     * This includes transitions from route overview to route following and continuously updating the camera as the location changes.
//     */
//    private lateinit var navigationCamera: NavigationCamera
//
//    /**
//     * Debug tool used to play, pause and seek route progress events that can be used to produce mocked location updates along the route.
//     */
//    private val mapboxReplayer = MapboxReplayer()
//
//    /**
//     * Debug tool that mocks location updates with an input from the [mapboxReplayer].
//     */
//    private val replayLocationEngine = ReplayLocationEngine(mapboxReplayer)
//
//    /**
//     * Debug observer that makes sure the replayer has always an up-to-date information to generate mock updates.
//     */
//    private val replayProgressObserver: ReplayProgressObserver =
//        ReplayProgressObserver(mapboxReplayer)
//
//    /**
//     * Generates updates for the [routeArrowView] with the geometries and properties of maneuver arrows that should be drawn on the map.
//     */
//    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi()
//
//    /**
//     * Generates updates for the [routeLineView] with the geometries and properties of the routes that should be drawn on the map.
//     */
//    private lateinit var routeLineApi: MapboxRouteLineApi
//
//    /**
//     * Draws route lines on the map based on the data from the [routeLineApi]
//     */
//    private lateinit var routeLineView: MapboxRouteLineView
//
//    /**
//     * Extracts message that should be communicated to the driver about the upcoming maneuver.
//     * When possible, downloads a synthesized audio file that can be played back to the driver.
//     */
//    private lateinit var speechApi: MapboxSpeechApi
//
//    private var haveDeparted = false
//
//
//    private var onCameraIdleListener : OnCameraIdleListener? = null
//
//
//    /**
//     * Gets notified with progress along the currently active route.
//     */
//    private val routeProgressObserver: RouteProgressObserver =
//        RouteProgressObserver { routeProgress -> // update the camera position to account for the progressed fragment of the route
//            if (isNavigationInProgress)
//            {
//                updateProgressOnTheMap(routeProgress)
//
//                val arrival = Calendar.getInstance()
//                arrival.add(Calendar.SECOND, routeProgress.durationRemaining.toInt())
//
//                updateApplicationState(routeProgress, arrival)
//            }
//        }
//
//
//    private fun updateApplicationState(routeProgress: RouteProgress, arrival: Calendar)
//    {
//        if (routeProgress.currentState == RouteProgressState.COMPLETE || routeProgress.fractionTraveled >= 1.0)
//        {
//            stopNavigation()
//            ApplicationStateData.getInstance().setTxtArrivalTime("--:--")
//            ApplicationStateData.getInstance().setArrivalTime(0)
//            ApplicationStateData.getInstance().setTxtRemainingDistance("--")
//            ApplicationStateData.getInstance().setTxtRemainingTime("--")
//            ApplicationStateData.getInstance().etaToStop = 0.0
//
//        }
//        else
//        {
//
//            ApplicationStateData.getInstance().setTxtRemainingDistance(formatDistance(routeProgress.distanceRemaining.toDouble()))
//
//            ApplicationStateData.getInstance().setTxtRemainingTime(formatTime(routeProgress.durationRemaining))
//
//            ApplicationStateData.getInstance().etaToStop = routeProgress.durationRemaining
//            ApplicationStateData.getInstance().setArrivalTime(arrival.timeInMillis)
//            ApplicationStateData.getInstance().setTxtArrivalTime(
//                    String.format(
//                        "%1$02d:%2$02d",
//                        arrival.get(Calendar.HOUR_OF_DAY),
//                        arrival.get(Calendar.MINUTE)
//                    )
//                )
//        }
//    }
//
//    private fun updateProgressOnTheMap(routeProgress: RouteProgress)
//    {
//        val style = mapView.getMapboxMap().getStyle()
//        if (style != null)
//        {
//            val maneuverArrowResult = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
//            routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
//
//            routeLineApi.updateWithRouteProgress(routeProgress) {
//                routeLineView.renderRouteLineUpdate(style, it)
//            }
//        } // update top banner with maneuver instructions
//        val maneuvers = maneuverApi.getManeuvers(routeProgress)
//        maneuvers.fold({ error ->
//            Toast.makeText(
//                ApplicationStateData.getInstance().applicationContext,
//                error.errorMessage,
//                Toast.LENGTH_SHORT
//            ).show()
//        }, {
//            maneuverView?.visibility = View.VISIBLE
//            maneuverView!!.renderManeuvers(maneuvers)
//        })
//    }
//
//    /**
//     * Gets notified whenever the tracked routes change.
//     *
//     * A change can mean:
//     * - routes get changed with [MapboxNavigation.setRoutes]
//     * - routes annotations get refreshed (for example, congestion annotation that indicate the live traffic along the route)
//     * - driver got off route and a reroute was executed
//     */
//    private val routesObserver: RoutesObserver = RoutesObserver { routeUpdateResult ->
//        if (routeUpdateResult.navigationRoutes.isNotEmpty())
//        { // generate route geometries asynchronously and render them
//
//            val routeLines =
//                routeUpdateResult.navigationRoutes.map { NavigationRouteLine(it, null) }
//
//            routeLineApi.setNavigationRouteLines(routeLines) { value ->
//                mapView.getMapboxMap().getStyle()?.apply {
//                    routeLineView.renderRouteDrawData(
//                        this,
//                        value
//                    )
//                }
//            }
//
//            LogUtil.LOGD(
//                TAG,
//                "routesObserver: Updating Location"
//            ) // update the camera position to account for the new route
//            viewportDataSource.onRouteChanged(routeUpdateResult.navigationRoutes.first())
//            viewportDataSource.evaluate()
//        }
//        else
//        {
//            clearRoutesAndArrow()
//        }
//
//    }
//
//    private fun clearRoutesAndArrow()
//    { // remove the route line and route arrow from the map
//        val style = mapView.getMapboxMap().getStyle()
//        if (style != null)
//        {
//            routeLineApi.clearRouteLine { value ->
//                routeLineView.renderClearRouteLineValue(
//                    style, value
//                )
//            }
//            routeArrowView.render(style, routeArrowApi.clearArrows())
//        }
//        maneuverView?.visibility = View.GONE // remove the route reference from camera position evaluations
//        viewportDataSource.clearRouteData()
//        viewportDataSource.evaluate()
//    }
//
//    /**
//     * When a synthesized audio file was downloaded, this callback cleans up the disk after it was played.
//     */
//    private val voiceInstructionsPlayerCallback =
//        MapboxNavigationConsumer<SpeechAnnouncement> { value -> // remove already consumed file to free-up space
//            speechApi.clean(value)
//        }
//
//    /**
//     * Based on whether the synthesized audio file is available, the callback plays the file
//     * or uses the fall back which is played back using the on-device Text-To-Speech engine.
//     */
//    private val speechCallback =
//        MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
//            expected.fold({ error -> // play the instruction via fallback text-to-speech engine
//                voiceInstructionsPlayer.play(
//                    error.fallback, voiceInstructionsPlayerCallback
//                )
//            }, { value -> // play the sound file from the external generator
//                if (isNavigationInProgress) voiceInstructionsPlayer.play(
//                    value.announcement, voiceInstructionsPlayerCallback
//                )
//            })
//        }
//
//    /**
//     * Observes when a new voice instruction should be played.
//     */
//    private val voiceInstructionsObserver: VoiceInstructionsObserver =
//        VoiceInstructionsObserver { voiceInstructions ->
//            if (isNavigationInProgress && (ApplicationStateData.getInstance().tts != null && !ApplicationStateData.getInstance().tts.isSpeaking))
//                speechApi.generate(voiceInstructions, speechCallback)
//        }
//
//    /*
//    *
//    *
//     */
//    private val arrivalObserver = object : ArrivalObserver
//    {
//        override fun onFinalDestinationArrival(routeProgress: RouteProgress)
//        {
//            LogUtil.LOGD(TAG, "arrival Observer onFinalDestinationArrival${routeProgress.currentState}")
//        }
//
//        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress)
//        {
//            LogUtil.LOGD(
//                TAG,
//                "arrival Observer onNextRouteLegStart${routeLegProgress.currentStepProgress}"
//            )
//        }
//
//        override fun onWaypointArrival(routeProgress: RouteProgress)
//        {
//            LogUtil.LOGD(TAG, "arrival Observer onWaypointArrival${routeProgress.currentState}")
//        }
//
//    }
//
//    /**
//     * Plays the synthesized audio files with upcoming maneuver instructions
//     * or uses an on-device Text-To-Speech engine to communicate the message to the driver.
//     */
//    private lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer
//
//    /**
//     * Draws maneuver arrows on the map based on the data [routeArrowApi].
//     */
//    private lateinit var routeArrowView: MapboxRouteArrowView
//
//    /**
//     * Generates updates for the [MapboxManeuverView] to display the upcoming maneuver instructions
//     * and remaining distance to the maneuver point.
//     */
//    private lateinit var maneuverApi: MapboxManeuverApi
//
//    /**
//     * Generates updates for the [MapboxTripProgressView] that include remaining time and distance to the destination.
//     */
//    private lateinit var tripProgressApi: MapboxTripProgressApi
//
//    private val offRouteProgressObserver: OffRouteObserver = OffRouteObserver { isOffRoute ->
//        if (isOffRoute)
//        {
//            val offset: Long = DateTimeUtil.getCurrentTimeInMillis() - lastOffrouteTime
//            if ( offset > OFF_ROUTE_RETRY)
//            {
//                LogUtil.LOGD(TAG, "User off route triggered")
//                lastOffrouteTime = DateTimeUtil.getCurrentTimeInMillis()
//                performRerouting()
//            }
//        }
//    }
//
//    init
//    {
//
//        LogUtil.LOGD(TAG,"INITIALIZING_MAPBOX")
//        initMapboxNavigation()
//
//        initPuckLocation()
//
//        initCamera()
//
//        // initialize maneuver arrow view to draw arrows on the map
//        val routeArrowOptions =
//            RouteArrowOptions.Builder(ApplicationStateData.getInstance().applicationContext).build()
//        routeArrowView = MapboxRouteArrowView(routeArrowOptions)
//
//        // initialize route line, the withRouteLineBelowLayerId is specified to place
//        // the route line below road labels layer on the map
//        // the value of this option will depend on the style that you are using
//        // and under which layer the route line should be placed on the map layers stack
//        val mapboxRouteLineOptions =
//            MapboxRouteLineOptions.Builder(ApplicationStateData.getInstance().applicationContext).withRouteLineBelowLayerId(
//                    "road-label"
//                ).build()
//        routeLineApi = MapboxRouteLineApi(mapboxRouteLineOptions)
//        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)
//
//        // make sure to use the same DistanceFormatterOptions across different features
//        val distanceFormatterOptions = mapboxNavigation.navigationOptions.distanceFormatterOptions
//
//        // initialize maneuver api that feeds the data to the top banner maneuver view
//        maneuverApi = MapboxManeuverApi(MapboxDistanceFormatter(distanceFormatterOptions))
//
//        initVoiceInstructions()
//
//        // initialize bottom progress view
//        tripProgressApi = MapboxTripProgressApi(
//            TripProgressUpdateFormatter.Builder(ApplicationStateData.getInstance().applicationContext).distanceRemainingFormatter(
//                    DistanceRemainingFormatter(distanceFormatterOptions)
//                ).timeRemainingFormatter(TimeRemainingFormatter(ApplicationStateData.getInstance().applicationContext)).percentRouteTraveledFormatter(
//                    PercentDistanceTraveledFormatter()
//                ).estimatedTimeToArrivalFormatter(
//                    EstimatedTimeToArrivalFormatter(
//                        ApplicationStateData.getInstance().applicationContext,
//                        TimeFormat.NONE_SPECIFIED
//                    )
//                ).build()
//        )
//
//        setupVoiceButton()
//    }
//
//    private fun initVoiceInstructions()
//    {
//        speechApi = MapboxSpeechApi(
//            ApplicationStateData.getInstance().applicationContext,
//            MAPBOX_ACCESS_TOKEN,
//            Locale.US.toLanguageTag()
//        )
//
//        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
//            ApplicationStateData.getInstance().applicationContext,
//            MAPBOX_ACCESS_TOKEN,
//            Locale.US.toLanguageTag()
//        )
//    }
//
//    private fun setupVoiceButton()
//    {
//        voiceToggleButton.setOnCheckedChangeListener { _, isChecked ->
//            voiceInstructionsPlayer.volume(SpeechVolume(if (!isChecked) 1.0f else 0.0f))
//        }
//    }
//
//    private fun performRerouting()
//    {
//
////        if (isNavigationInProgress) {
////            navigate(
////                Point.fromLngLat(
////                    ApplicationStateData.getInstance().destination.longitude(),
////                    ApplicationStateData.getInstance().destination.latitude()
////                )
////            )
////        }else{
////            navigate(
////                Point.fromLngLat(
////                    LocationUtil.getCurrentLatLng().lng,
////                    LocationUtil.getCurrentLatLng().lat
////                )
////            )
////        }
////        try
////        {
////            LogUtil.d(TAG,"Performing OFFROUTE")
////            if (!ApplicationStateData.getInstance().unitSession.isRideSharing)
////            {
////                if (MeterService.dispatchJob != null)
////                {/*
////                If taxi is ON CALL then we need to navigate
////                to the pickup first else if its not we will go for
////                dropOff
////                 */
////                    if (MeterService.iMeter.taxiState == Config.TaxiStatus.ON_CALL)
////                    {
////                        if (MeterService.dispatchJob != null && MeterService.dispatchJob.getPickupLocation() != null && MeterService.dispatchJob.getPickupLocation().isNotEmpty())
////                        {
////                            navigate(
////                                Point.fromLngLat(
////                                    MeterService.dispatchJob.pickupLon,
////                                    MeterService.dispatchJob.pickupLat
////                                )
////                            )
////                        }
////                    }
////                    else
////                    {
////                        if (MeterService.dispatchJob != null && MeterService.iMeter != null && !MeterService.iMeter.isMeterFree && MeterService.dispatchJob.getDropLocation() != null && MeterService.dispatchJob.getDropLocation().isNotEmpty())
////                        {
////                            navigate(
////                                Point.fromLngLat(
////                                    MeterService.dispatchJob.dropLon,
////                                    MeterService.dispatchJob.dropLat
////                                )
////                            )
////                        }
////                    }
////                }
////                else
////                {
////                    LogUtil.d(TAG, "No next location available to navigate")
////                }
////            }
////            else if (MeterService.iMeter.taxiState == Config.TaxiStatus.ON_CALL)
////            {
////                if (MeterService.dispatchJob != null && MeterService.dispatchJob.getPickupLocation() != null && MeterService.dispatchJob.getPickupLocation().isNotEmpty())
////                {
////                    navigate(
////                        Point.fromLngLat(
////                            MeterService.dispatchJob.pickupLon,
////                            MeterService.dispatchJob.pickupLat
////                        )
////                    )
////                }
////            }
////            else
////            {
////                if (NavigationManager.getInstance().getNextDestination() != null)
////                {
////                    navigate(NavigationManager.getInstance().getNextDestination())
////                }
////            }
////        } catch (ex: Exception)
////        {
////            LogUtil.d(TAG, ex.message)
////        }
//    }
//
////    @SuppressLint("MissingPermission")
////    fun navigateToFixedRoute(destination: Point?,encodedPath: String) {
////        this.destination = destination
////
////        registerObserver()
////
////        try {
////            if (NetworkAvailabilityUtil.isConnected()) {
////                val currentLatLng = LocationUtil.getCurrentLatLng()
////                isNavigationInProgress = true
////                startNavigation(
////                    destination,
////                    encodedPath
////                )
////            } else LogUtil.LOGE(TAG, "Skipping navigation because of offline mode")
////        } catch (e: Exception) {
////            LogUtil.LOGE(TAG, e.message)
////            e.printStackTrace()
////        }
////    }
//
//
//    private fun startNavigation(destination: Point?,encodedPath: String) {
//        try {
//            destination?.let { navigateToFixedRoute(it,encodedPath) }
//            isNavigationInProgress = true
//        } catch (e: Exception) {
//            e.printStackTrace()
//            LogUtil.LOGE(TAG, e.message)
//        }
//    }
//     fun navigateToFixedRoute(destination: Point,encodedPath: String?){
//         registerObserver()
//        val originLocation = ApplicationStateData.getInstance().currentLocation
//        val originPoint = Point.fromLngLat(
//            ApplicationStateData.getInstance().currentLocation.longitude,
//            ApplicationStateData.getInstance().currentLocation.latitude
//        )
//        val list = PolyUtil.decode("}zwxCoadyHiAiCRM\\S\\S^S\\Uf@Yh@[vAu@RKGSaAgCM_@o@kBCYMa@I]CIAMBKHKDGFEf@YXQlEgCl@YJGz@c@nF}Cf@YLIHODK?K@IJ?HCIBK??IAIAGCIaDsJi@aB~EwBdAc@Tv@nDtM`AlD");
//
//        val finalList = filterPoints(list)
//
//        val mapMatching = MapboxMapMatching.builder()
//            .accessToken("pk.eyJ1Ijoic2FtYXNoIiwiYSI6ImNrMG94ZmR6MTAwY2MzZ210ZGNvajBhMXYifQ.JACwZ07i2-LQIcm9znH_4w")
//            .coordinates(finalList)
//            .voiceInstructions(true)
//            .steps(true)
//            .bannerInstructions(true)
//            .profile(DirectionsCriteria.PROFILE_DRIVING)
//            .build()
//
//        mapMatching.enqueueCall(object : Callback<MapMatchingResponse> {
//            override fun onResponse(
//                call: Call<MapMatchingResponse>,
//                response: Response<MapMatchingResponse>
//            ) {
//
//                if (response.isSuccessful) {
//                    response.body()?.matchings()?.let { matchingList ->
//                        if (matchingList.isNotEmpty()) {
//                            val matchedPoint = matchingList.first()
//                            val  directionsRoute = DirectionsRoute.builder()
//                                .legs(matchedPoint.legs())
//                                .duration(matchedPoint.duration())
//                                .distance(matchedPoint.distance())
//                                .routeIndex("0")
//                                .geometry(matchedPoint.geometry())
//                                .routeOptions(RouteOptions
//                                    .builder()
//                                    .applyDefaultNavigationOptions()
//                                    .voiceUnits(UnitType.METRIC.value)
//                                    .language(Locale.ENGLISH.toLanguageTag())
//                                    .coordinatesList(listOf(originPoint,destination))
//                                    .bearingsList(listOf(Bearing.builder().angle(originLocation.bearing.toDouble()).degrees(45.0).build(), null))
//                                    .build())
//                                .build();
//
//                            mapboxNavigation.setNavigationRoutes(listOf(directionsRoute.toNavigationRoute()))
//                        }
//                    }
//                }
//            }
//
//            override fun onFailure(call: Call<MapMatchingResponse>, t: Throwable) {
//                LogUtil.LOGD(TAG, "Route can not found" + t.localizedMessage)
//            }
//        })
//    }
//
//    private fun filterPoints(mList: List<LatLng>): List<Point> {
//        val shuffledList = mutableListOf<Point>()
//
//        if (mList.isNotEmpty()) {
//            var previousLatLng = mList[0]
//            var currentIndex = 1
//
//            while (currentIndex < mList.size) {
//                var currentLatLng = mList[currentIndex]
//
//                // Keep updating currentLatLng until the distance is >= 100 meters
//                while (haversine(
//                        previousLatLng.latitude,
//                        previousLatLng.longitude,
//                        currentLatLng.latitude,
//                        currentLatLng.longitude
//                    ) < 100.0
//                ) {
//                    currentIndex++ // Move to the next point
//                    if (currentIndex >= mList.size) {
//                        break // Break if we've reached the end of the list
//                    }
//                    currentLatLng = mList[currentIndex]
//                }
//
//                // Add the current point to the shuffled list
//                shuffledList.add(Point.fromLngLat(currentLatLng.longitude, currentLatLng.latitude))
//                previousLatLng = currentLatLng
//            }
//        }
//
//        return shuffledList
//    }
//
//    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
//        val R = 6371000.0 // Earth radius in meters
//        val dLat = Math.toRadians(lat2 - lat1)
//        val dLon = Math.toRadians(lon2 - lon1)
//        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
//                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
//                Math.sin(dLon / 2) * Math.sin(dLon / 2)
//        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
//        return R * c
//    }
//    private fun unregisterObservers()
//    {
//        LogUtil.LOGD(TAG, "Unregistering observers $mapboxNavigation")
//        mapboxNavigation.unregisterRoutesObserver(routesObserver)
//        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
//        mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
//        mapboxNavigation.unregisterOffRouteObserver(offRouteProgressObserver)
//        mapboxNavigation.unregisterArrivalObserver(arrivalObserver)
//    }
//
//    fun registerLocationObserver(){
//        ApplicationStateData.getInstance().registerLocationObserver(this)
//    }
//    private fun registerObserver()
//    {
//        LogUtil.LOGD(TAG, "Registering observers $mapboxNavigation") // register event listeners
//        mapboxNavigation.registerRoutesObserver(routesObserver)
//        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
//        mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
//        mapboxNavigation.registerOffRouteObserver(offRouteProgressObserver)
//        mapboxNavigation.registerArrivalObserver(arrivalObserver)
//    }
//
//    private fun initCamera()
//    {
//        viewportDataSource = MapboxNavigationViewportDataSource(mapView.getMapboxMap())
//
//        viewportDataSource.onLocationChanged(ApplicationStateData.getInstance().currentLocation)
//        navigationCamera = NavigationCamera(
//            mapView.getMapboxMap(), mapView.camera, viewportDataSource
//        )
//        viewportDataSource.evaluate()
//    }
//
//    private fun initMapboxNavigation()
//    {
//        mapboxNavigation = if (MapboxNavigationProvider.isCreated())
//        {
//            MapboxNavigationProvider.retrieve()
//        }
//        else
//        {
//            MapboxNavigationProvider.create(
//                NavigationOptions.Builder(ApplicationStateData.getInstance().applicationContext).accessToken(MAPBOX_ACCESS_TOKEN).locationEngine( LocationEngineProvider.getBestLocationEngine(ApplicationStateData.getInstance().applicationContext))
//                    .build()
//            )
//        }
//    }
//
//    private fun initPuckLocation()
//    {
//        mapView.location.apply {
//            this.locationPuck = LocationPuck2D(
//                bearingImage = ContextCompat.getDrawable(
//                    ApplicationStateData.getInstance().applicationContext,
//                    R.drawable.mapbox_navigation_puck_icon
//                )
//            )
//            setLocationProvider(navigationLocationProvider)
//            enabled = true
//        }
//
//
//        mapView.getMapboxMap().addOnMoveListener(object : OnMoveListener {
//            override fun onMove(detector: MoveGestureDetector): Boolean {
//                onCameraIdleListener?.onCameraIdle(null)
//              return false
//            }
//
//            override fun onMoveBegin(detector: MoveGestureDetector) {
//                onCameraIdleListener?.onCameraIdle(null)
//            }
//
//            override fun onMoveEnd(detector: MoveGestureDetector) {
//                val option = mapView.getMapboxMap().cameraState.center
//                if (option.latitude() > 0.0 && option.longitude() > 0.0) {
//                    onCameraIdleListener?.let {
//                        it.onCameraIdle(Location("").also {
//                            it.latitude = option.latitude()
//                            it.longitude = option.longitude()
//                        })
//                    }
//                } else {
//                    onCameraIdleListener?.onCameraIdle(null)
//                }
//            }
//        })
//
//
//    }
//
//    private fun findRoute(destination: Point)
//    {
//        val originLocation = ApplicationStateData.getInstance().currentLocation
//        val originPoint = Point.fromLngLat(
//            ApplicationStateData.getInstance().currentLocation.longitude,
//            ApplicationStateData.getInstance().currentLocation.latitude
//        )
//
//        LogUtil.LOGD(TAG, "Navigating from: " + originPoint.latitude() + "," + originPoint.longitude())
//        LogUtil.LOGD(TAG, "Navigating to: " + destination.latitude() + "," + destination.longitude())
//
//        // execute a route request
//        // it's recommended to use the
//        // applyDefaultNavigationOptions and applyLanguageAndVoiceUnitOptions
//        // that make sure the route request is optimized
//        // to allow for support of all of the Navigation SDK features
//        navigationRouteId =
//            mapboxNavigation.requestRoutes(
//                RouteOptions.builder().applyDefaultNavigationOptions().applyLanguageAndVoiceUnitOptions(
//                    ApplicationStateData.getInstance().applicationContext
//                ).coordinatesList(
//                    listOf(
//                        originPoint,
//                        destination
//                    )
//                ) // provide the bearing for the origin of the request to ensure
//                // that the returned route faces in the direction of the current user movement
//                .bearingsList(
//                    listOf(
//                        Bearing.builder().angle(originLocation.bearing.toDouble()).degrees(45.0).build(),
//                        null
//                    )
//                ).build(),
//
//                object : NavigationRouterCallback
//                {
//                    override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: RouterOrigin)
//                    {
//                        setRouteAndStartNavigation(routes)
//                        isNavigationInProgress = true
//                    }
//
//                    override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions)
//                    {
//                        LogUtil.LOGE(TAG, reasons.toString())
//                        ApplicationStateData.getInstance().speak("Couldn't find the route")
//                    }
//
//                    override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin)
//                    { // no impl
//                    }
//                })
//    }
//
//    @SuppressLint("MissingPermission") private fun setRouteAndStartNavigation(routes: List<NavigationRoute>)
//    { // set routes, where the first route in the list is the primary route that
//        // will be used for active guidance
//        mapboxNavigation.setNavigationRoutes(routes)
//        navigationCamera.requestNavigationCameraToFollowing()
//        LogUtil.LOGD(TAG,"Starting trip session")
//        mapboxNavigation.startTripSession(false)
//    }
//
//    @SuppressLint("MissingPermission") fun navigate(destination: Point?)
//    {
//        this.destination = destination
//
//        registerObserver()
//
//        try
//        {
//            if (NetworkAvailabilityUtil.isConnected())
//            {
//                val currentLatLng = LocationUtil.getCurrentLatLng()
//                LogUtil.LOGD("MapApplication","Current : ${currentLatLng.lat}, ${currentLatLng.lng}")
//                LogUtil.LOGD("MapApplication","=========================")
//                LogUtil.LOGD("MapApplication","Destination: $destination")
//                isNavigationInProgress = true
//                startNavigation(
//                    Point.fromLngLat(currentLatLng.lng, currentLatLng.lat),
//                    destination
//                )
//            }
//            else LogUtil.LOGE(TAG, "Skipping navigation because of offline mode")
//        } catch (e: Exception)
//        {
//            LogUtil.LOGE(TAG, e.message)
//            e.printStackTrace()
//        }
//    }
//
//    @SuppressLint("MissingPermission") fun onStart()
//    {
//
//        if (::mapboxNavigation.isInitialized && mapboxNavigation.isDestroyed.not())
//        {
//            //and recenter camera in Trip Session
//            if (isTripStarted)
//            {
//                LogUtil.LOGD(TAG,"Starting trip session")
//                registerObserver()
//                mapboxNavigation.startTripSession(true)
//                //processNavigation()
//            }
//            else
//            {
//               clearNavigation()
//            }
//        }
//    }
//
//    private fun clearNavigation(){
//        LogUtil.LOGD(TAG, "Removing the navigation")
//        stopNavigation()
//        clearRoutesAndArrow()
//    }
//
//    fun onStop()
//    {
//        LogUtil.LOGD(TAG,"MAPBOX ONSTOP")
//
//        if (::mapboxNavigation.isInitialized || mapboxNavigation.isDestroyed.not())
//        {
//            unregisterObservers()
//            ApplicationStateData.getInstance().registerLocationObserver(null)
//            speechApi.cancel()
//            voiceInstructionsPlayer.shutdown()
//            clearRoutesAndArrow()
//        }
//    }
//
//    /**
//     * Start navigation between two points and multiple
//     * way points
//     * multiple way points are also stops so we will also
//     * add this in our navigation
//     * @param origin this is the starting point for the navigation
//     * @param destination this is the ending point for the navigation
//     * both origin and destination will keep updating according to
//     * the available stops in ride sharing case
//     * @param wayPoints is the list of the navigation points from
//     * start to end
//     * @param context is the calling activity
//     */
//    private fun startNavigation(origin: Point?, destination: Point?)
//    {
//        try
//        {
//            destination?.let { findRoute(it) }
//            isNavigationInProgress = true
//        } catch (e: Exception)
//        {
//            e.printStackTrace()
//            LogUtil.LOGE(TAG, e.message)
//        }
//    }
//
//    fun recenterMap()
//    {
//        LogUtil.LOGD(TAG, "recenterMap: ")
//        val cameraOptions = CameraOptions.Builder().center(
//            Point.fromLngLat(
//                ApplicationStateData.getInstance().currentLocation.longitude,
//                ApplicationStateData.getInstance().currentLocation.latitude
//            )
//        ).zoom(17.0).build()
//        navigationLocationProvider.changePosition(ApplicationStateData.getInstance().currentLocation)
//        mapView.getMapboxMap().setCamera(cameraOptions)
//
//    }
//
//    fun stopNavigation()
//    {
//
//        unregisterObservers()
//        clearRoutesAndArrow()
//        navigationRouteId?.let { it ->
//            mapboxNavigation.cancelRouteRequest(it)
//        }
//
//        if (::voiceInstructionsPlayer.isInitialized)
//        {
//            voiceInstructionsPlayer.clear()
//            voiceInstructionsPlayer.shutdown()
//        }
//        if (::speechApi.isInitialized)
//        {
//            speechApi.cancel()
//        }
//
//        if (::mapboxNavigation.isInitialized && !mapboxNavigation.isDestroyed)
//        {
//            mapboxNavigation.stopTripSession()
//           mapboxNavigation.resetTripSession()
//        }
//
//        ApplicationStateData.getInstance().destination = null
//
//        if (isNavigationInProgress)
//            isNavigationInProgress = false
//
//
//
//    }
//
//    companion object
//    {
//        private val TAG = MapApplication::class.java.simpleName
//    }
//
//
////    fun performRerouting()
////    {
////        try
////        {
////            LogUtil.LOGD(TAG,"Performing OFFROUTE")
////            if (!ApplicationStateData.getInstance().unitSession.isRideSharing)
////            {
////                if (MeterService.LOGDispatchJob != null)
////                {/*
////                If taxi is ON CALL then we need to navigate
////                to the pickup first else if its not we will go for
////                dropOff
////                 */
////                    if (MeterService.iMeter.taxiState == Config.TaxiStatus.ON_CALL)
////                    {
////                        if (MeterService.LOGDispatchJob != null && MeterService.LOGDispatchJob.getPickupLocation() != null && MeterService.LOGDispatchJob.getPickupLocation().isNotEmpty())
////                        {
////                            navigate(
////                                Point.fromLngLat(
////                                    MeterService.LOGDispatchJob.pickupLon,
////                                    MeterService.LOGDispatchJob.pickupLat
////                                )
////                            )
////                        }
////                    }
////                    else
////                    {
////                        if (MeterService.LOGDispatchJob != null && MeterService.iMeter != null && !MeterService.iMeter.isMeterFree && MeterService.LOGDispatchJob.getDropLocation() != null && MeterService.LOGDispatchJob.getDropLocation().isNotEmpty())
////                        {
////                            navigate(
////                                Point.fromLngLat(
////                                    MeterService.LOGDispatchJob.LOGDropLon,
////                                    MeterService.LOGDispatchJob.LOGDropLat
////                                )
////                            )
////                        }
////                    }
////                }
////                else
////                {
////                    LogUtil.LOGD(TAG, "No next location available to navigate")
////                }
////            }
////            else if (MeterService.iMeter.taxiState == Config.TaxiStatus.ON_CALL)
////            {
////                if (MeterService.LOGDispatchJob != null && MeterService.LOGDispatchJob.getPickupLocation() != null && MeterService.LOGDispatchJob.getPickupLocation().isNotEmpty())
////                {
////                    navigate(
////                        Point.fromLngLat(
////                            MeterService.LOGDispatchJob.pickupLon,
////                            MeterService.LOGDispatchJob.pickupLat
////                        )
////                    )
////                }
////            }
////            else
////            {
////                if (NavigationManager.getInstance().getNextDestination() != null)
////                {
////                    navigate(NavigationManager.getInstance().getNextDestination())
////                }
////            }
////        } catch (ex: Exception)
////        {
////            LogUtil.LOGD(TAG, ex.message)
////        }
////    }
//
//    private fun formatTime(seconds: Double): String
//    {
//        if (seconds < 60)
//        {
//            return String.format("%1$.0f sec", seconds)
//        }
//        if (seconds < (60 * 2)) // less than 2 minutes
//        {
//            return String.format("%1$.0f min<br>%2$.0f sec", seconds / 60, seconds % 60)
//        }
//
//        return if (seconds < (60 * 60)) // less than 1 hour
//        {
//            String.format("%1$.0f min", seconds / 60, seconds % 60)
//        }
//        else
//        {
//            String.format(
//                "%1$.0f hr<br>%2$.0f min", seconds / (60 * 60), (seconds % (60 * 60)) / 60
//            )
//        }
//    }
//
//
//    private fun formatDistance(meters: Double): String
//    {
//        return if (meters < 1000)
//        {
//            String.format("%.0f m", meters);
//        }
//        else if (meters < 10000)
//        {
//            String.format("%.1f km", meters / 1000.0);
//        }
//        else
//        {
//            String.format("%.0f km", meters / 1000.0);
//        }
//    }
//
//    override fun onNewLocation(location: Location?)
//    {
//        LogUtil.LOGD("OnNewLocation","Location: ${location?.latitude} , ${location?.longitude}")
//
//        navigationLocationProvider.changePosition(
//            location = location!!,
//            keyPoints = emptyList(),
//        )
//
//        if (System.currentTimeMillis() - mapCameraRecenterTimer > MAPBOX_DELAY_TIMER)
//        {
//            mapCameraRecenterTimer = System.currentTimeMillis()
//            //sanitizeNavigation()
//
//            if (isFirstTime)
//            {
//                isFirstTime = false
//                viewportDataSource.followingZoomPropertyOverride(17.0)
//                viewportDataSource.followingPadding = EdgeInsets(0.0, 0.0, ImageUtil.dpToPx(250).toDouble(), 0.0)
//            }
//
//        }
//        navigationCamera.requestNavigationCameraToFollowing()
//
//        if (location != null)
//        {
//            viewportDataSource.onLocationChanged(location)
//        }
//        viewportDataSource.evaluate()
//
//
//    }
//
//    fun setCurrentLocation(location: MDTLocation?) {
//        location?.let {
//            mapView.getMapboxMap().easeTo(
//                CameraOptions.Builder()
//                    .center(Point.fromLngLat(location.lng, location.lat))
//                    .zoom(17.0)
//                    .padding(EdgeInsets(500.0, 0.0, 0.0, 0.0))
//                    .build()
//            )
//
//
//        }
//    }
//
//    private fun sanitizeNavigation() {
//        if(isTripStarted){
//            if (::mapboxNavigation.isInitialized) {
//                if (mapboxNavigation.getTripSessionState() == TripSessionState.STARTED) {
//                    LogUtil.LOGD(TAG, "removing stale navigation")
//                    stopNavigation()
//                    clearRoutesAndArrow()
//                }
//            }
//        }
//
//    }
//
//    fun registerCameraMoveLocationObserver(listener : OnCameraIdleListener){
//        onCameraIdleListener = listener
//    }
//
//    interface OnCameraIdleListener{
//        fun onCameraIdle(location: Location?)
//    }
//}
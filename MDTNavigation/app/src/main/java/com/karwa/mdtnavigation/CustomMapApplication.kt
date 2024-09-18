package com.karwa.mdtnavigation

import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.*
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import java.util.*

class CustomMapApplication(
    var mapView: MapView,
    var maneuverView: MapboxManeuverView?
) : KLocationObserver {
    private var destination: Point? = null
    var isNavigationInProgress = false
    private var mapCameraRecenterTimer = 0L
    private val MAPBOX_DELAY_TIMER: Long = 5000
    private var lastOffrouteTime: Long = 0
    private val OFF_ROUTE_RETRY: Long = 5000
    private var lastDestinationPoint: Point? = null
    private lateinit var context: Context
    private var navigationRouteId: Long?=null

    /**
     * Mapbox Navigation entry point.
     */
    private lateinit var mapboxNavigation: MapboxNavigation

    /**
     * Navigation camera and viewport data.
     */
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    private lateinit var navigationCamera: NavigationCamera

    /**
     * Route arrow and line rendering utilities.
     */
    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi()
    private lateinit var routeLineApi: MapboxRouteLineApi
    private lateinit var routeLineView: MapboxRouteLineView

    /**
     * Polyline Points List (Now Un-chunked for Testing)
     */
    lateinit var listOfPoints: List<Point>
    lateinit var currentList: List<Point>
    var currentIndex = -1

    private lateinit var routeArrowView: MapboxRouteArrowView


    init {
        initMapboxNavigation()
        initPuckLocation()
        initCamera()
        initRouteRendering()

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
                ).build()
        routeLineApi = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

        // make sure to use the same DistanceFormatterOptions across different features
        val distanceFormatterOptions = mapboxNavigation.navigationOptions.distanceFormatterOptions

        // initialize maneuver api that feeds the data to the top banner maneuver view
//        maneuverApi = MapboxManeuverApi(MapboxDistanceFormatter(distanceFormatterOptions))
//
//        initVoiceInstructions()
//
//        // initialize bottom progress view
//        tripProgressApi = MapboxTripProgressApi(
//            TripProgressUpdateFormatter.Builder(ApplicationStateData.getInstance().applicationContext)
//                .distanceRemainingFormatter(
//                    DistanceRemainingFormatter(distanceFormatterOptions)
//                )
//                .timeRemainingFormatter(TimeRemainingFormatter(ApplicationStateData.getInstance().applicationContext))
//                .percentRouteTraveledFormatter(
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

    }

    private fun initMapboxNavigation() {
        mapboxNavigation = if (MapboxNavigationProvider.isCreated()) {
            MapboxNavigationProvider.retrieve()
        } else {
            MapboxNavigationProvider.create(
                NavigationOptions.Builder(context)
                    .accessToken(ApplicationStateData.getInstance().getString(R.string.mapbox_access_token))
                    .build()
            )
        }
    }

    private fun initPuckLocation() {
        mapView.location.apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    context,
                    com.mapbox.navigation.R.drawable.mapbox_navigation_puck_icon
                )
            )
            setLocationProvider(NavigationLocationProvider())
            enabled = true
        }
    }

    private fun initCamera() {
        viewportDataSource = MapboxNavigationViewportDataSource(mapView.getMapboxMap())
        navigationCamera = NavigationCamera(
            mapView.getMapboxMap(), mapView.camera, viewportDataSource
        )
        viewportDataSource.evaluate()
    }

    private fun initRouteRendering() {
        // Initialize route arrow view
        routeLineApi = MapboxRouteLineApi(
            MapboxRouteLineOptions.Builder(context)
                .withRouteLineBelowLayerId("road-label")
                .build()
        )
        routeLineView = MapboxRouteLineView(
            MapboxRouteLineOptions.Builder(context)
                .withRouteLineBelowLayerId("road-label")
                .build()
        )
    }

    fun listOfTTestLatLng() = arrayListOf<LatLng>(
        LatLng(25.195265577583886, 51.47176668047905),
        LatLng(25.195440020214022, 51.47173583507538),
        LatLng(25.195527696502502, 51.471695601940155),
        LatLng(25.1956942509389, 51.472125090658665),
        LatLng(25.195933008904877, 51.47275038063526),
        LatLng(25.196446625153648, 51.473939940333366),
        LatLng(25.196777607660582, 51.47473119199276),
        LatLng(25.197280298894984, 51.47601127624512),
        LatLng(25.197521177363026, 51.47648099809885),
        LatLng(25.197839718686037, 51.477158926427364),
        LatLng(25.198039944234033, 51.477561593055725),
        LatLng(25.198313281912718, 51.47752337157726),
        LatLng(25.19902438172289, 51.47716294974089),
        LatLng(25.199841959350344, 51.476745530962944),
        LatLng(25.200342817738104, 51.47652089595795),
        LatLng(25.200674396385256, 51.47650212049484),
        LatLng(25.201020232255527, 51.47670563310385),
        LatLng(25.201405504392888, 51.47742077708244),
        LatLng(25.201815044304478, 51.47841017693281),
        LatLng(25.202687511404108, 51.48026257753372),
        LatLng(25.20323386192235, 51.481220461428165),
        LatLng(25.203760795107748, 51.482264176011086),
        LatLng(25.20439966440823, 51.483983136713505),
        LatLng(25.205273933107744, 51.486020274460316),
        LatLng(25.205972251479192, 51.4875702559948),
        LatLng(25.20690687548934, 51.48984409868717),
        LatLng(25.207548761759522, 51.49152651429176),
        LatLng(25.209291787456337, 51.49479813873768),
        LatLng(25.211054201911818, 51.49715647101402),
        LatLng(25.214920200673458, 51.50060947984457),
        LatLng(25.21854372361527, 51.50369267910719),
        LatLng(25.224114080579547, 51.50802545249462),
        LatLng(25.22890653693186, 51.51313304901123),
        LatLng(25.231009261944482, 51.515430361032486),
        LatLng(25.233728222851052, 51.51855546981096),
        LatLng(25.236034666836876, 51.52152668684721),
        LatLng(25.23782216873608, 51.52296803891659),
        LatLng(25.24045879219972, 51.52413781732321),
        LatLng(25.24315661685926, 51.524431854486465),
        LatLng(25.245812229513774, 51.524326242506504),
        LatLng(25.248707651512564, 51.524411737918854),
        LatLng(25.25160088184092, 51.52441542595625),
        LatLng(25.25495374058694, 51.52448281645775),
        LatLng(25.259707685157526, 51.52442917227745),
        LatLng(25.26210338679993, 51.524617932736874),
        LatLng(25.262935397676056, 51.52463871985674),
        LatLng(25.262940249034713, 51.525962725281715),
        LatLng(25.262876574936968, 51.52781546115875),
        LatLng(25.26289628358984, 51.529860980808735),
        LatLng(25.26289143222943, 51.53087317943573),
        LatLng(25.262870510735436, 51.531917564570904),
        LatLng(25.262870207525346, 51.532199531793594),
        LatLng(25.26299149149862, 51.53220221400261),
        LatLng(25.263035760118672, 51.531776413321495),
        LatLng(25.26316432095117, 51.53135899454355),
        LatLng(25.263840172711422, 51.531333178281784),
        LatLng(25.26460637605609, 51.53133284300566),
        LatLng(25.265348621459808, 51.53134860098362),
        LatLng(25.2659083345595, 51.53128791600466),
        LatLng(25.266579016630093, 51.53123024851084),
        LatLng(25.267367942604913, 51.53099253773689),
        LatLng(25.268041648583004, 51.530933529138565),
        LatLng(25.26841912847094, 51.53095465153456),
        LatLng(25.268552231541026, 51.53095968067646),
        LatLng(25.268606503593777, 51.530797742307186),
        LatLng(25.268546774014833, 51.53065290302038),
        LatLng(25.268616812249117, 51.530255265533924),
        LatLng(25.268684121682927, 51.529758386313915),
        LatLng(25.268746579952985, 51.529103592038155),
        LatLng(25.268842389664034, 51.528566144406796),
        LatLng(25.268879985859105, 51.528223156929016),
        LatLng(25.268888475320907, 51.527797020971775)
    )


    // Start Navigation with un-chunked list
    @SuppressLint("MissingPermission")
    fun startNavigation(destination: Point?, context: Context) {
        this.context = context

        // Get list of points (Un-chunked for testing)
        listOfPoints = listOfTTestLatLng().map { LatLng ->
            Point.fromLngLat(LatLng.longitude, LatLng.latitude)
        }

        currentIndex = -1

        updateRoute {
            Log.d("MapApplication", "Route Updated Successfully")
        }
    }

    // Filter points that are too close together
    private fun filterClosePoints(points: List<Point>, distanceThreshold: Double): List<Point> {
        val filteredPoints = mutableListOf<Point>()
        points.forEachIndexed { index, point ->
            if (index == 0 || index == points.size - 1) {
                filteredPoints.add(point) // Always add first and last point
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

    // Update and draw route
    private fun updateRoute(onSuccess: () -> Unit) {
        currentIndex += 1
        if (currentIndex < listOfPoints.size) {
            currentList =
                filterClosePoints(listOfPoints, 50.0) // Filter points with distance threshold

            clearRoutesAndArrow()

            findRoute(onSuccess)
        } else {
            Log.e("MapApplication", "No more points in route")
        }
    }

    // Clear route and arrows before drawing new
    fun clearRoutesAndArrow() {
        val style = mapView.getMapboxMap().getStyle()
        if (style != null) {
            routeLineApi.clearRouteLine { value ->
                routeLineView.renderClearRouteLineValue(style, value)
            }
            routeArrowApi.clearArrows()
        }
    }

    // Find and draw route based on filtered points
    private fun findRoute(onSuccess: () -> Unit) {
        if (navigationRouteId != null) {
            mapboxNavigation.cancelRouteRequest(navigationRouteId!!)
        }

        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .coordinatesList(currentList)
                .waypointIndicesList(
                    listOf(
                        0,
                        currentList.size - 1
                    )
                ) // Only use first and last point as waypoints
                .build(),
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    setRouteAndStartNavigation(routes)
                    onSuccess()
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    Log.e("MapApplication", "Failed to get routes: $reasons")
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    Log.e("MapApplication", "Route request canceled")
                }
            }
        )
    }

    // Set route and start navigation
    @SuppressLint("MissingPermission")
    private fun setRouteAndStartNavigation(routes: List<NavigationRoute>) {
        mapboxNavigation.setNavigationRoutes(routes)
        navigationCamera.requestNavigationCameraToFollowing()
        mapboxNavigation.startTripSession(true)
    }

    // Helper function to calculate distance between points
    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    // Unregister observers to clean up navigation
    private fun unregisterObservers() {
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterArrivalObserver(arrivalObserver)
    }

    // Function to register required observers
    private fun registerObservers() {
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerArrivalObserver(arrivalObserver)
    }

    // Observer for route progress
    private val routeProgressObserver: RouteProgressObserver =
        RouteProgressObserver { routeProgress ->
            val fractionTraveled = routeProgress.fractionTraveled
            Log.d("MapApplication", "Fraction Traveled: $fractionTraveled")
            updateProgressOnTheMap(routeProgress)
        }

    // Observer for route updates
    private val routesObserver: RoutesObserver = RoutesObserver { routeUpdateResult ->
//        if (routeUpdateResult.navigationRoutes.isNotEmpty()) {
//            drawRoute(routeUpdateResult.navigationRoutes)
//        } else {
//            clearRoutesAndArrow()
//        }
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
//        val maneuvers = maneuverApi.getManeuvers(routeProgress)
//        maneuvers.fold({ error ->
//
//        }, {
//            maneuverView?.visibility = View.VISIBLE
//            maneuverView!!.renderManeuvers(maneuvers)
//        })
    }

    // Observer for arrival
    private val arrivalObserver = object : ArrivalObserver {
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            Log.d("MapApplication", "Arrived at final destination")
        }

        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {

        }

        override fun onWaypointArrival(routeProgress: RouteProgress) {
        }
    }

    override fun onNewLocation(location: android.location.Location?) {

    }
}

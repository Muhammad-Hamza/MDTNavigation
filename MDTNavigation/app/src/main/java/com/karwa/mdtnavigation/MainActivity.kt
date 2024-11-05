package com.karwa.mdtnavigation

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.karwa.mdtnavigation.databinding.MainActivityBinding
import com.karwa.mdtnavigation.log.FirebaseLogger
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask


class MainActivity : AppCompatActivity() {
    lateinit var binding: MainActivityBinding
    var mapApplication: MapApplication? = null
    lateinit var looger: FirebaseLogger
    private lateinit var requestStoragePermission: ActivityResultLauncher<String>

    //    var mapApplication: CustomMapApplication? = null
    val MY_PERMISSIONS_REQUEST_READ_LOCATION = 7
    private lateinit var requestLocationPermission: ActivityResultLauncher<String>
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var tapCount = 0
    private val tapThreshold = 7
    private var lastTapTime: Long = 0
    private val tapInterval = 500 // milliseconds

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
        initBlinkingEffect()
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


        binding.llCurrentLocation.setOnClickListener {
            mapApplication?.calculateOffRouting(true)
        }
        binding.ivCancel.setOnClickListener({
            finish()
        })
        binding.btnOffRoute.setOnClickListener({
            looger.logSelectContent("Button Click", "Off Route", "Off Route button clicked")
            mapApplication?.increaseOffRouteCount()
            mapApplication?.calculateOffRouting(true)
        })

        binding.btnDrawNextLayout.setOnClickListener {
            looger.logSelectContent("Button Click", "Draw Next", "Draw Next Layout button clicked")
            binding.btnDrawNextLayout.visibility = View.GONE
            mapApplication!!.startNextRoute(true)
        }
        binding.btnWaze.setOnClickListener {
            mapApplication?.openWazeApp()
        }

        requestStoragePermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    // Permission granted, proceed with file copy operation
                    showCopyDialog()
                } else {
                    // Permission denied, show a message to the user
                    Toast.makeText(
                        this,
                        "Storage permission is required to copy files.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        binding.bottomBarLayout.arrivalTimeText.setOnClickListener {
            val currentTime = System.currentTimeMillis()

            // Check if taps are within the allowed interval
            if (currentTime - lastTapTime <= tapInterval) {
                tapCount++
            } else {
                tapCount = 1 // Reset count if the interval is exceeded
            }

            lastTapTime = currentTime

            if (tapCount >= tapThreshold) {
                checkAndRequestStoragePermission()
                tapCount = 0 // Reset after hitting the threshold
            }
        }
    }

    private fun checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Check for storage permission on Android 10 and below
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission already granted
                showCopyDialog()
            } else {
                // Request permission
                requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            // Android 11 and above, no storage permission required for Downloads
            showCopyDialog()
        }
    }

    fun getBytesArrayFromStringBuilder(stringBuilder: StringBuilder): ByteArray {
        // Convert StringBuilder to String and then to ByteArray
        return stringBuilder.toString()
            .toByteArray(Charsets.UTF_8) // You can specify the charset if needed
    }

    fun readStringFromFile(context: Context, file: File): StringBuilder? {
        val stringBuilder = StringBuilder()

        return try {
            val inputStream = FileInputStream(file)
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Read the file line by line
            reader.use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    stringBuilder.append(line).append("\n") // Append each line
                }
            }

            stringBuilder // Return the StringBuilder with content
        } catch (e: Exception) {
            e.printStackTrace() // Log the exception
            null // Return null if there's an error
        }
    }

    private fun showCopyDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Coping logs file")
        builder.setMessage("We are coping logs please wait.....")

        val dialog = builder.create()

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()

        GlobalScope.launch(Dispatchers.IO) {
            delay(2000L)

            val internalStorageDir = filesDir

            // Check if internal storage directory exists
            if (internalStorageDir.exists() && internalStorageDir.isDirectory) {
                // List all files in the internal storage directory
                val files = internalStorageDir.listFiles() ?: null

                if (files != null && files.size > 0) {
                    val downloadsDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val customFolder = File(downloadsDir, "mdt_nav_logs")

                    // Create the folder if it doesn't exist
                    if (!customFolder.exists()) {
                        customFolder.mkdirs() // Create the folder
                    }

                    val listOfFiles = ArrayList<File>()
                    for (file in files) {
                        if (file.name.endsWith("_event_logs.txt")) {
                            try {


                                // Define the initial destination file path
                                var destinationFile = File(customFolder, file.name)

                                // Check if the file already exists
//                                if (destinationFile.exists()) {
                                // Append a timestamp before the file extension
                                val fileNameWithoutExtension = file.nameWithoutExtension
                                val fileExtension = file.extension
                                val timestamp =
                                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(
                                        Date()
                                    )

                                // Create a new file name with timestamp
                                destinationFile = File(
                                    customFolder,
                                    "${fileNameWithoutExtension}_$timestamp.$fileExtension"
                                )
//                                }

                                // Copy the file contents to the new location
                                FileInputStream(file).use { inputStream ->
                                    FileOutputStream(destinationFile).use { outputStream ->
                                        inputStream.copyTo(outputStream) // Use copyTo for easier file copying
                                    }
                                }
                                listOfFiles.add(file)
                            } catch (e: Exception) {
                                Log.e("FileCopy", "Failed to copy ${file.name}", e)
                            }
                        }
                    }

                    if (listOfFiles.size > 0) {
                        deleteFilesSafely(listOfFiles)
                    }
                }
                withContext(Dispatchers.Main) {
                    delay(2000L)
                    dialog.dismiss()
                }
            } else {
                Log.e("FileCopy", "Internal storage directory does not exist or is not a directory")
            }

        }
    }

    fun deleteFilesSafely(listOfFiles: List<File>): Int {
        var deletedCount = 0

        for (file in listOfFiles) {
            try {
                if (file.exists() && file.delete()) {
                    deletedCount++ // Increment count if the file was deleted successfully
                }
            } catch (e: Exception) {
                e.printStackTrace() // Log the exception (optional)
                // Handle any specific exception types if needed
            }
        }

        return deletedCount // Return the count of successfully deleted files
    }
//    @Throws(IOException::class)
//    private fun copyFile(sourceFile: File, destFile: File) {
//        FileInputStream(sourceFile).use { input ->
//            FileOutputStream(destFile).use { output ->
//                input.copyTo(output)
//            }
//        }
//    }


    fun copyFileToDownloads(context: Context, sourceFile: File) {
        val resolver: ContentResolver = context.contentResolver
        val originalFileName = sourceFile.nameWithoutExtension
        val fileExtension = sourceFile.extension
        var fileName = "$originalFileName.$fileExtension"
        var fileUri: Uri?
        var outputStream: OutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var unique = false
            var suffix = 1

            // Loop to check for existing files and create a unique filename
            while (!unique) {
                val queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val cursor = resolver.query(
                    queryUri,
                    arrayOf(MediaStore.Downloads._ID),
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                    arrayOf(fileName),
                    null
                )

                if (cursor != null && cursor.moveToFirst()) {
                    // File exists, create a new filename with a suffix
                    fileName = "$originalFileName(${suffix++}).$fileExtension"
                    cursor.close()
                } else {
                    unique = true
                }
            }

            // Create the ContentValues with the unique file name
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download")
            }

            // Insert the file into MediaStore
            fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (fileUri != null) {
                outputStream = resolver.openOutputStream(fileUri)
            }
        } else {
            // For Android 9 and below, use external storage directly
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, fileName)
            outputStream = FileOutputStream(destFile)
        }

        // Copy file contents
        outputStream?.use { outStream ->
            FileInputStream(sourceFile).use { inStream ->
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (inStream.read(buffer).also { bytesRead = it } != -1) {
                    outStream.write(buffer, 0, bytesRead)
                }
            }
        }
    }

    fun copyFileContents(inputStream: InputStream, outputStream: OutputStream) {
        val buffer = ByteArray(1024)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }
    }

    private fun copyFileContents(sourceFile: File, outputStream: OutputStream?) {
        FileInputStream(sourceFile).use { input ->
            outputStream?.use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun startNavigation() {
        initMap()
        startServices()
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            runOnUiThread {
                val intent = intent
                var route =
                    "ewwxCuvdyHO_@I]CIAM??BKAKEIGGE?G?GBEBCFAD?D??EHEDi@l@KPaFrC_@LWDWFC?OCIC??iBcGOk@a@qAwAwEEQGQGQEOoAcEeGkRaAcDyFqQ_C}HkEeN]gAuA}EGU??CYAU@QDQFMFMHMJMLKJExJcErB}@bOkG??rA_At@_@nEmBHE??DCDGBGBMIU]gAeCuH_B{EMa@[}@K_@u@wB??"
//                if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
//                    val route: String = intent.data?.getQueryParameter("ROUTE_INTENT").toString()
//                    Route 1
//                val route ="ewwxCuvdyHO_@I]CIAM??BKAKEIGGE?G?GBEBCFAD?D??EHEDi@l@KPaFrC_@LWDWFC?OCIC??iBcGOk@a@qAwAwEEQGQGQEOoAcEeGkRaAcDyFqQ_C}HkEeN]gAuA}EGU??CYAU@QDQFMFMHMJMLKJExJcErB}@bOkG??rA_At@_@nEmBHE??DCDGBGBMIU]gAeCuH_B{EMa@[}@K_@u@wB??"

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
//                }
            }
        }, 1000)
    }

    private fun initBlinkingEffect() {

//        binding.shimmerViewContainer.apply {
//            setShimmer(
//                Shimmer.ColorHighlightBuilder()
//                    .setBaseAlpha(0.5f) // Lower alpha to keep shimmer subtle
//                    .setHighlightAlpha(0.1f)
//                    .setDuration(1500L)  // Duration for shimmer cycle
//                    .build()
//            )
//            startShimmer()
//        }

//        val blinkAnimation = AlphaAnimation(0.0f, 1.0f).apply {
//            duration = 1000 // duration for each blink (in milliseconds)
//            repeatMode = Animation.REVERSE
//            repeatCount = Animation.INFINITE
//        }
//
//        binding.btnWaze.startAnimation(blinkAnimation)
    }

    private fun startServices() {
        val intent = Intent(this, BackgroundLocationService::class.java)
//        val intent = Intent(this, NewBackgroundLocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
//        startForegroundService(Intent(this, BackgroundLocationService::class.java))
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
                binding.rlWaze,
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

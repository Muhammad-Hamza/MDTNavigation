package com.karwa.mdtnavigation;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

/**
 * Created by KSAdmin on 3/16/2016.
 */
public class BackgroundLocationService extends Service
{

    public static final String CHANNEL_ID = "1703198712";
    private static final String TAG = "BackgroundLocationService";
    private Location lastLocation = null;


    private LocationRequest locationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private static boolean isServiceRunning = false;
    private LocationCallback locationCallback;
    private static final float MIN_BEARING_CHANGE = 25;
    private static final float MIN_BEARING_CHANGE_IN_NEGATIVE = -25;

    @Override
    public void onCreate()
    {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        requestLocation();

        setNotificationAndStartForeground();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        // handler.removeCallbacks(sendUpdatesToUI);
        isServiceRunning = false;

        super.onDestroy();


        if (mFusedLocationClient != null)
        {
            mFusedLocationClient.removeLocationUpdates(locationCallback);
        }


    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

    }

    private void setNotificationAndStartForeground()
    {
        createNotificationChannel(getApplicationContext());
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(),
               CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // notification icon
                .setContentTitle("MDT") // title for notification
                .setContentText("MDT has been started...") // message for notification
                .setAutoCancel(true); // clear notification after click

        mBuilder.setContentIntent(pendingIntent);

        startForeground(2, mBuilder.build());
    }

    public static void createNotificationChannel(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationManager mNotificationManager = context.getSystemService(NotificationManager.class);

            NotificationChannel existingChannel = mNotificationManager.getNotificationChannel(CHANNEL_ID);

            if (existingChannel == null)
            {
                CharSequence name = "MDT_location_service";
                String description = "Channel used for mdt";
                int importance = NotificationManager.IMPORTANCE_HIGH;

                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                channel.setDescription(description);
                channel.enableLights(true);
                channel.enableVibration(true);
                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static boolean bringingForegroundTaskDone = true;

    private Location sentLocation = null;
    private Location lastLocationSent = null;

    private void updateStatusUrgentlyIfRequired(Location location)
    {
        if (sentLocation == null)
        {
            sentLocation = location;
        }


    }

    private boolean isBearing(Location lastSyncedLocation, Location lastLocation)
    {
        float lastSyncedLocationBearing = lastSyncedLocation.getBearing();
        float currentLocationbearing = lastLocation.getBearing();


        float bearingDiff = (lastSyncedLocationBearing - currentLocationbearing);

        return bearingDiff < MIN_BEARING_CHANGE_IN_NEGATIVE || bearingDiff > MIN_BEARING_CHANGE;
    }

    private void sendLocationToServer()
    {
//        if (lastLocationSent == null)
//        {
//            lastLocationSent = sentLocation;
//        }
//
//        lastLocationSent = sentLocation;
//        new StatusUpdateResponseProcessorThread(getApplicationContext(), null).start();
//        ApplicationStateData.getInstance().setLastLocaxtionTime(System.currentTimeMillis());
    }

    @SuppressLint("MissingPermission")
    private void requestLocation()
    {
        if (!isServiceRunning)
        {
            this.locationRequest = LocationRequest.create();
            this.locationRequest.setInterval(1000);
            this.locationRequest.setFastestInterval(1000);
            this.locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            this.locationCallback = new LocationCallback()
            {
                @Override
                public void onLocationResult(LocationResult locationResult)
                {
                    Location location = locationResult.getLastLocation();
                    if(ApplicationStateData.Companion.getInstance().getLocationObserver() != null)
                        ApplicationStateData.Companion.getInstance().getLocationObserver().onNewLocation(location);

                    updateStatusUrgentlyIfRequired(location);

                    ApplicationStateData.Companion.getInstance().setCurrentLocation(location);


                    lastLocation = location;

                }
            };

            this.mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {

                return;
            }
            this.mFusedLocationClient.requestLocationUpdates(this.locationRequest, this.locationCallback, Looper.myLooper());
            isServiceRunning = true;
        }
    }
}
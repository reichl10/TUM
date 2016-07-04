package com.esrlabs.geofence;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.esrlabs.headunitinterface.HeadUnit;

import static android.location.LocationManager.NETWORK_PROVIDER;
import static com.esrlabs.geofence.Utils.location;

/**
 * Steps:
 * - register to the location provider
 * - connect to the HeadUnitService
 * - build geofence object
 * - implement the exercise logic (when the user is outside of the geofence show popup; hide it otherwise)
 *
 * See:
 * - that tests are green
 * - that the notification appears in the emulator
 */
public class GeofenceApp extends Service implements LocationListener {

    public static final String TAG = "GeofenceApp";

    private LocationManager locationManager;
    private LocationListener lListener;
    private Geofence geofence;
    private HeadUnit huService;

    public GeofenceApp(LocationManager manager, CircleGeofence fence){
        this.locationManager = manager;
        this.geofence = fence;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate");

        if (locationManager == null) {
            locationManager = (LocationManager)
                    this.getSystemService(Context.LOCATION_SERVICE);
        }

        initLocationListener();
        initHeadUnitService();

        if (geofence == null) {
            final int someRadiusInMeters = 25;
            final Location someCenterForTheGeofence = location(NETWORK_PROVIDER, 48.118920, 11.601057);
            geofence = new CircleGeofence(someCenterForTheGeofence, someRadiusInMeters);
        }
    }

    private void initLocationListener(){
        locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER, 0, 0, this);
    }

    private void initHeadUnitService(){
        ServiceConnection service;
        service = new ServiceConnection(){

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                huService = HeadUnit.Stub.asInterface(iBinder);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.e(TAG, "Service disconnected");
                huService = null;
            }
        };
        Intent headUnitServiceIntent = new Intent(HeadUnit.class.getName());
        headUnitServiceIntent.setPackage("com.esrlabs.headunitservice");
        bindService(headUnitServiceIntent, service, BIND_AUTO_CREATE);
    }

    public Location latestLocation(){
        return this.locationManager.getLastKnownLocation(locationManager.NETWORK_PROVIDER);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (!geofence.containsLocation(location)){
            try {
                huService.showNotification("Out of boundaries!!");
            } catch (RemoteException e) {
                Log.e(TAG, "Display out-of-boundaries failed");
            }
        }
        else{
            try {
                huService.hideAllNotifications();
            } catch (RemoteException e) {
                Log.e(TAG, "Hiding notifications failed");
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
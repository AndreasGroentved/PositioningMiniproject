package com.group.unkown.positioningminiproject.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.group.unkown.positioningminiproject.R;
import com.group.unkown.positioningminiproject.domain.LocationHandler;
import com.group.unkown.positioningminiproject.domain.LocationStrength;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.EddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleEddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.kontakt.sdk.android.common.profile.IEddystoneNamespace;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.group.unkown.positioningminiproject.model.Beacon;
import com.group.unkown.positioningminiproject.model.BuildingModel;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_INTERNET = 200;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final float ZOOM_LEVEL = 16f;
    private static final String GPS_TAG = "GPS";

    private GoogleMap map;
    private boolean isMapReady = false;
    private ProximityManager proximityManager;
    private LocationManager mLocationManager;
    private LocationHandler mLocationHandler;
    private LatLng currentLatLng;

    private Map<String, LocationStrength> locationStrengthMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationHandler = new LocationHandler();
        locationStrengthMap = new HashMap<>();
        InputStream ins = getResources().openRawResource(getResources().getIdentifier("beacons", "raw", getPackageName()));
        new BuildingModel(ins);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        new AlertDialog.Builder(this)
                .setTitle("Title")
                .setMessage("sup?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Prompt the user once explanation has been shown
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                MY_PERMISSIONS_REQUEST_LOCATION);
                        startKontakting();
                    }
                })
                .create()
                .show();


        new AlertDialog.Builder(this)
                .setTitle("Title")
                .setMessage("sup?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Prompt the user once explanation has been shown
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.INTERNET}, REQUEST_INTERNET
                        );
                        startKontakting();
                    }
                })
                .create()
                .show();

        new AlertDialog.Builder(this)
                .setTitle("Title")
                .setMessage("sup?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Prompt the user once explanation has been shown
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                                REQUEST_ENABLE_BT);
                        startKontakting();
                    }
                })
                .create()
                .show();
    }

    private void startKontakting() {
        if (!permissionsGranted()) {
            Log.i("sup", "permissions failed ");
            return;
        }
        Log.i("sup", "permissions enabled");
        KontaktSDK.initialize(this);

        listen();
        proximityManager = ProximityManagerFactory.create(this);
        proximityManager.setIBeaconListener(createIBeaconListener());
        proximityManager.setEddystoneListener(createEddystoneListener());
        startScanning();
    }

    private void locationUpdate() {
        if (locationStrengthMap.size() < 1) return;
        currentLatLng = mLocationHandler.getNearestNeighbor(new ArrayList<>(locationStrengthMap.values()));
        Log.i("sup", "current position " + currentLatLng.latitude + "," + currentLatLng.longitude);
        drawMarker(currentLatLng);
    }

    public void centerOnLocation(View view) {
        if (currentLatLng == null || !isMapReady) return;
        
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, ZOOM_LEVEL));
    }


    private void drawMarker(LatLng marker) {
        if (!isMapReady) return;
        map.clear();
        map.addMarker(new MarkerOptions().position(marker));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        isMapReady = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!permissionsGranted()) {
            return;
        }
        startKontakting();
    }

    private boolean permissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!permissionsGranted()) {
            return;
        }
        proximityManager.stopScanning();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!permissionsGranted()) {
            return;
        }
        proximityManager.disconnect();
        proximityManager = null;

    }

    private void startScanning() {
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                proximityManager.startScanning();
            }
        });
    }

    private IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                Log.i("Sample", "IBeacon discovered: " + ibeacon.getUniqueId());

                Beacon beacon = BuildingModel.getBeacon(ibeacon.getUniqueId());
                Log.i("found", "" + beacon);
                if (beacon == null) return;
                if (beacon.getLatitude() == 0.0d || beacon.getLongitude() == 0.0d) return;
                LocationStrength locationStrength = new LocationStrength(new LatLng(beacon.getLatitude(), beacon.getLongitude()), (float) ibeacon.getRssi());

                locationStrengthMap.put(ibeacon.getUniqueId(), locationStrength);

                locationUpdate();
            }

            @Override
            public void onIBeaconLost(IBeaconDevice ibeacon, IBeaconRegion region) {
                Log.i("Sample", "IBeacon lost: " + ibeacon.toString());

                Beacon beacon = BuildingModel.getBeacon(ibeacon.getUniqueId());
                if (beacon == null) return;

                locationStrengthMap.remove(ibeacon.getUniqueId());

                locationUpdate();
            }
        };
    }

    private EddystoneListener createEddystoneListener() {
        return new SimpleEddystoneListener() {
            @Override
            public void onEddystoneDiscovered(IEddystoneDevice eddystone, IEddystoneNamespace namespace) {
                Log.i("Sample", "Eddystone discovered: " + eddystone.toString());
            }
        };
    }


    private final LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            LatLng gpsLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            locationStrengthMap.put(GPS_TAG, new LocationStrength(gpsLatLng, location.getAccuracy() * -1));
            locationUpdate();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            switch (i) {
                case LocationProvider.OUT_OF_SERVICE:
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    locationStrengthMap.remove(GPS_TAG);
                    locationUpdate();
            }
        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }

    };

    protected boolean pickGps() {
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    protected boolean check() {
        if (!pickGps()) {
            Log.i("sup", "GPS not available");
        }
        return pickGps();
    }

    @SuppressLint("MissingPermission")
    protected void listen() {

        if (!check()) {
            return;
        }
        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location != null) {
            currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        }

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }
}




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
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.group.unkown.positioningminiproject.R;
import com.group.unkown.positioningminiproject.domain.LocationHandler;
import com.group.unkown.positioningminiproject.domain.LocationInfo;
import com.group.unkown.positioningminiproject.domain.LocationStrength;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.EddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleEddystoneListener;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.kontakt.sdk.android.common.profile.IEddystoneNamespace;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;

import com.group.unkown.positioningminiproject.model.Beacon;
import com.group.unkown.positioningminiproject.model.BuildingModel;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_INTERNET = 200;
    private static final int REQUEST_LOCATION = 99;
    private static final int REQUEST_BT = 1;
    private static final float DEFAULT_ZOOM_LEVEL = 16f;
    private static final String GPS_TAG = "GPS";

    private static final String fileName = "positioning.csv";

    private enum LocationType {
        GPS, BLE, COMBINED
    };

    private GoogleMap map;
    private static final String LOG_STRING = "Positioning";
    private boolean isMapReady = false;
    private ProximityManager proximityManager;
    private LocationManager mLocationManager;
    private LocationHandler mLocationHandler;
    private LocationInfo currentLatLng;
    private LatLng currentBtPosition;
    private LatLng currentGpsPosition;
    private TextView btCountView;
    private TextView currentlyUsingView;
    private boolean firstUpdate = true;
    private FileOutputStream logFileOutputStream;

    private Map<String, LocationStrength> locationStrengthMap;
    private List<LocationStrength> locationStrengths;

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
        btCountView = findViewById(R.id.number_of_bluetooth_beacons);
        currentlyUsingView = findViewById(R.id.currently_using);

        File logFile = new File(Environment.getExternalStorageDirectory(), fileName);

        try {
            logFileOutputStream = new FileOutputStream(logFile, true);
            Log.i(LOG_STRING, "File: " + logFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (!permissionsGranted()) askPermissions();

    }

    private void logLocation(LocationInfo locationInfo, LocationType locationType) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(Calendar.getInstance().getTime().toString()).append(",").
                    append(locationType.name()).append(",").
                    append(locationInfo.latLng.latitude + "").append(",").
                    append(locationInfo.latLng.longitude + "").append("\n");
            logFileOutputStream.write(sb.toString().getBytes());
            logFileOutputStream.flush();
            Log.i(LOG_STRING, "write to log file " + sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void askPermissions() {
        makePermissionDialog(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        makePermissionDialog(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        makePermissionDialog(new String[]{Manifest.permission.INTERNET}, REQUEST_INTERNET);
        makePermissionDialog(new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_BT);
    }

    private void makePermissionDialog(final String[] permissionStringArray, final int somePermissionInt) {
        new AlertDialog.Builder(this)
                .setTitle("Title")
                .setMessage("sup?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                permissionStringArray,
                                somePermissionInt);
                        startKontakting();
                    }
                })
                .create()
                .show();
    }

    private void startKontakting() {
        if (!permissionsGranted()) {
            Log.i(LOG_STRING, "permissions failed ");
            return;
        }
        Log.i(LOG_STRING, "permissions enabled");
        KontaktSDK.initialize(this);

        listen();
        proximityManager = ProximityManagerFactory.create(this);
        proximityManager.setIBeaconListener(createIBeaconListener());
        proximityManager.setEddystoneListener(createEddystoneListener());
        startScanning();
    }

    private void locationUpdate() {
        if (locationStrengthMap.size() < 1) return;
        LocationInfo current = mLocationHandler.getNearestNeighbor(new ArrayList<>(locationStrengthMap.values()));
        if (current.latLng == null) return;
        currentLatLng = current;
        drawMarker(current, LocationType.COMBINED);
        updateInfoViews(current.numberOfBeacons);
        if (firstUpdate) {
            centerOnLocation(this.btCountView);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(current.latLng, DEFAULT_ZOOM_LEVEL));
            firstUpdate = false;
        }
    }

    public void centerOnLocation(View view) {
        if (currentLatLng == null || !isMapReady) return;

        map.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng.latLng));
    }


    private void drawMarker(LocationInfo locationInfo, LocationType locationType) {
        if (!isMapReady) return;
        //map.clear();

        switch (locationType) {
            case BLE:
                map.addMarker(new MarkerOptions().position(locationInfo.latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                break;
            case GPS:
                map.addMarker(new MarkerOptions().position(locationInfo.latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                break;
            case COMBINED:
                map.addMarker(new MarkerOptions().position(locationInfo.latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                break;
        }
        logLocation(locationInfo, locationType);
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
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
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

        try {
            logFileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        return new IBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                Log.i(LOG_STRING, "IBeacon discovered: " + ibeacon.getUniqueId());
                Log.i(LOG_STRING, "" + (BuildingModel.getBeacon(ibeacon.getUniqueId()) != null));
            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {
                updateBtLocations(iBeacons);
            }

            @Override
            public void onIBeaconLost(IBeaconDevice ibeacon, IBeaconRegion region) {
                Log.i(LOG_STRING, "IBeacon lost: " + ibeacon.toString());
                Beacon beacon = BuildingModel.getBeacon(ibeacon.getUniqueId());
                if (beacon == null) return;
                locationStrengthMap.remove(ibeacon.getUniqueId());
            }
        };
    }
                
    private void updateBtLocations(List<IBeaconDevice> iBeacons) {
        List<LocationStrength> beacons = new ArrayList<>();
        for (IBeaconDevice iBeaconDevice : iBeacons) {
            Beacon beacon = BuildingModel.getBeacon(iBeaconDevice.getUniqueId());
            if (beacon != null) {
                LocationStrength locationStrength = new LocationStrength(new LatLng(beacon.getLatitude(), beacon.getLongitude()), (float) iBeaconDevice.getRssi());
                beacons.add(locationStrength);
                locationStrengthMap.put(iBeaconDevice.getUniqueId(), locationStrength);
                locationUpdate();
            }
        }

        LocationInfo locationInfo = mLocationHandler.getNearestNeighbor(beacons);

        if (locationInfo == null) {
            Log.i(LOG_STRING, "no bluetooth positions");
            if (currentGpsPosition == null) Log.i(LOG_STRING, "No available positioning source");
            return;
        }
        currentBtPosition = locationInfo.latLng;
        Log.i(LOG_STRING, "currentBtPosition bt position " + currentBtPosition.latitude + "," + currentBtPosition.longitude);
        updateInfoViews(locationInfo.numberOfBeacons);
        drawMarker(locationInfo, LocationType.BLE);
    }

    private void updateInfoViews(int numOfBtDevices) {
        currentlyUsingView.setText("Using");
        btCountView.setText(numOfBtDevices + " devices");
    }

    private void removeBeacon(double latitude, double longitude) {
        int removeIndex = -1;
        for (int i = 0; i < locationStrengths.size(); i++) {
            if (locationStrengths.get(i).latLng.longitude == longitude && locationStrengths.get(i).latLng.latitude == latitude) {
                removeIndex = i;
                break;
            }
        }
        if (removeIndex != -1) {
            Log.i(LOG_STRING, "beacon removed");
            locationStrengths.remove(removeIndex);
        }
    }

    private EddystoneListener createEddystoneListener() {
        return new SimpleEddystoneListener() {
            @Override
            public void onEddystoneDiscovered(IEddystoneDevice eddystone, IEddystoneNamespace namespace) {
                Log.i(LOG_STRING, "Eddystone discovered: " + eddystone.toString());
            }
        };
    }

    private final LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            LatLng gpsLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            locationStrengthMap.put(GPS_TAG, new LocationStrength(gpsLatLng, location.getAccuracy() * -1));
            locationUpdate();
            drawMarker(new LocationInfo(gpsLatLng, 0), LocationType.GPS);
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
            Log.i(LOG_STRING, "GPS not available");
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
            currentGpsPosition = new LatLng(location.getLatitude(), location.getLongitude());
        }

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }
}

package com.group.unkown.positioningminiproject.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.kontakt.sdk.android.common.profile.IEddystoneNamespace;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import Model.Beacon;
import Model.BuildingModel;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private static final String LOG_STRING = "Positioning";
    private boolean isMapReady = false;
    private ProximityManager proximityManager;
    private static final int REQUEST_INTERNET = 200;
    public static final int REQUEST_LOCATION = 99;
    private static final int REQUEST_BT = 1;
    private LocationManager lm;
    private static final float DEFAULT_ZOOM_LEVEL = 19f;
    private LocationHandler mLocationHandler;
    private List<LocationStrength> locationStrengths;
    private LatLng currentBtPosition;
    private LatLng currentGpsPosition;
    private TextView btCountView;
    private TextView currentlyUsingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationHandler = new LocationHandler();
        locationStrengths = new ArrayList<>();
        InputStream ins = getResources().openRawResource(getResources().getIdentifier("beacons", "raw", getPackageName()));

        new BuildingModel(ins);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        btCountView = findViewById(R.id.number_of_bluetooth_beacons);
        currentlyUsingView = findViewById(R.id.currently_using);

        if (!permissionsGranted()) askPermissions();

    }

    private void askPermissions() {
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


    private void centerOnLocation(LatLng latLng) {
        if (latLng == null || !isMapReady) return;
        float zoomLevel = map.getCameraPosition().zoom; /*19f;*/

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));
    }


    private void drawMarker(LatLng marker, boolean isGps) {
        if (!isMapReady) return;
        if (isGps)
            map.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        else
            map.addMarker(new MarkerOptions().position(marker).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM_LEVEL));
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
                Log.i(LOG_STRING, "IBeacon discovered: " + ibeacon.getUniqueId());
                Log.i(LOG_STRING, "" + (BuildingModel.getBeacon(ibeacon.getUniqueId()) != null));
                updateBtLocations(ibeacon, false);
            }

            @Override
            public void onIBeaconLost(IBeaconDevice ibeacon, IBeaconRegion region) {
                Log.i(LOG_STRING, "IBeacon lost: " + ibeacon.toString());
                updateBtLocations(ibeacon, true);
            }
        };
    }

    private void updateBtLocations(IBeaconDevice ibeacon, boolean remove) {
        Beacon beacon = BuildingModel.getBeacon(ibeacon.getUniqueId());
        if (beacon != null) {
            LocationStrength locationStrength = new LocationStrength(new LatLng(beacon.getLatitude(), beacon.getLongitude()), (float) ibeacon.getRssi());
            if (remove) removeBeacon(beacon.getLatitude(), beacon.getLongitude());
            else locationStrengths.add(locationStrength);
        }

        LocationInfo locationInfo = mLocationHandler.getNearestNeighbor(locationStrengths);

        if (locationInfo == null) {
            Log.i(LOG_STRING, "no bluetooth positions");
            if (currentGpsPosition == null) Log.i(LOG_STRING, "No available positioning source");
            centerOnLocation(currentGpsPosition);
            drawMarker(currentGpsPosition, true);
            updateInfoViews(0);
            return;
        }
        currentBtPosition = locationInfo.latLng;
        Log.i(LOG_STRING, "currentBtPosition bt position " + currentBtPosition.latitude + "," + currentBtPosition.longitude);
        centerOnLocation(currentBtPosition);
        updateInfoViews(locationInfo.numberOfBeacons);
        drawMarker(currentBtPosition, false);
    }

    private void updateInfoViews(int numOfBtDevices) {
        if (numOfBtDevices == 0) currentlyUsingView.setText("Using GPS");
        else currentlyUsingView.setText("Using Bluetooh");
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


    private final LocationListener ll = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            currentGpsPosition = new LatLng(location.getLatitude(), location.getLongitude());
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    };


    protected boolean pickGps() {
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
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
        Location l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (l != null) {
            currentGpsPosition = new LatLng(l.getLatitude(), l.getLongitude());
        }

        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, ll);
    }
}




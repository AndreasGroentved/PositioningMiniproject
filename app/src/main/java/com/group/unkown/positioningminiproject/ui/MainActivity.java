package com.group.unkown.positioningminiproject.ui;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.group.unkown.positioningminiproject.R;

public class MainActivity extends AppCompatActivity   {

    protected LocationManager lm;
    protected double lat, lng;
    protected static final int LOCATION_PERMISSION = 2;
    protected TextView latView;
    protected TextView lngView;

    protected void onCreate(Bundle savedInstanceState) {
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        permission();
        listen();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latView = (TextView) findViewById(R.id.lat);
        lngView = (TextView) findViewById(R.id.lng);


    }

    protected void permission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION);

            }

        }


    }

    private final LocationListener ll = new LocationListener() {


        @Override
        public void onLocationChanged(Location location) {

            lat = location.getLatitude();
            lng = location.getLongitude();

            latView.setText("lat " + lat);
            lngView.setText("lng " + lng);

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

        if(!pickGps())  {
            showAlert();
        }

        return pickGps();
    }

    protected void listen() {

        if(!check())  {
            return;
        }

        else {
            Location l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(l != null){
                lat = l.getLatitude();
                lng = l.getLongitude();
            }

            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,4000,0,ll);
        }

    }

    protected void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .

                        setPositiveButton("Location Settings", new
                                DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                        startActivity(myIntent);
                                    }
                                })

                .

                        setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            }
                        });
        dialog.show();

    }
}




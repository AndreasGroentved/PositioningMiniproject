package com.group.unkown.positioningminiproject.domain;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Andreas Grøntved on 18-03-2018.
 **/

public class LocationInfo {
    public LatLng latLng;
    public int numberOfBeacons = 0;

    public LocationInfo(LatLng latLng, int numberOfBeacons) {
        this.latLng = latLng;
        this.numberOfBeacons = numberOfBeacons;
    }
}

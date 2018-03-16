package com.group.unkown.positioningminiproject.domain;

import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Andreas Grøntved on 15-03-2018.
 **/

public class LocationStrength {
    public LatLng latLng;
    public Float strength;

    public LocationStrength(@NonNull LatLng latLng, @NonNull Float strength) {
        this.latLng = latLng;
        this.strength = strength;
    }
}

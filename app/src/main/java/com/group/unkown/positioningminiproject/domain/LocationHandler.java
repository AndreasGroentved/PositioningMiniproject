package com.group.unkown.positioningminiproject.domain;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Andreas Grøntved on 15-03-2018.
 **/

public final class LocationHandler {

    public static final int DEFAULT_N = 2;

    @Nullable
    public LocationInfo getNearestNeighbor(@NonNull List<LocationStrength> dbs) {
        return getNearestNeighbor(DEFAULT_N, dbs);
    }

    @Nullable
    public LocationInfo getNearestNeighbor(int n, @NonNull List<LocationStrength> dbs) {
        if (dbs.isEmpty()) return null;

        List<LocationStrength> filteredList = new ArrayList<>(dbs.size());
        for (LocationStrength ls : dbs) {
            if (ls.latLng.longitude != 0) filteredList.add(ls);
        }
        if (filteredList.isEmpty()) return null;

        Collections.sort(filteredList, locationStrengthSorter);
        if (filteredList.size() <= n) n = filteredList.size() - 1;
        List<LocationStrength> nearest = filteredList.subList(0, n);

        double sumLat = 0;
        double sumLng = 0;
        for (LocationStrength loc : nearest) {
            sumLat += loc.latLng.latitude;
            sumLng += loc.latLng.longitude;
        }

        return new LocationInfo(new LatLng(sumLat / n, sumLng / n), n);
    }

    private Comparator<LocationStrength> locationStrengthSorter = new Comparator<LocationStrength>() {
        @Override
        public int compare(LocationStrength o1, LocationStrength o2) {
            return o2.strength.compareTo(o1.strength);
        }
    };
}

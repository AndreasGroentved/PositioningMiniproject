package Model;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Arpad on 3/13/2018.
 */

public class BuildingModel {

    private static Map<String, Beacon> beacons = new HashMap<>();

    public BuildingModel(InputStream ins) {

        Gson gson = new Gson();

        Beacon[] beaconArray = {};

        try (InputStreamReader reader = new InputStreamReader(ins)) {
            beaconArray = gson.fromJson(reader, Beacon[].class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Beacon beacon : beaconArray) {
            beacons.put(beacon.getAlias(), beacon);
            Log.i("Beacon", beacon.getAlias());
        }


    }

    public static Beacon getBeacon(@Nullable String alias) {
        if (beacons.containsKey(alias)) {
            return beacons.get(alias);
        } else {
            return null;
        }
    }
}

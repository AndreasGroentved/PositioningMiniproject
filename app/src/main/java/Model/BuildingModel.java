package Model;

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

    private Map<String, Beacon> beacons = new HashMap<>();

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
        }

    }

    public Beacon getBeacon(String alias) {
        if(beacons.containsKey(alias)) {
            return beacons.get(alias);
        } else {
            return null;
        }
    }
}

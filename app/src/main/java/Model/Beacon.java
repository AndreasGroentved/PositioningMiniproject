package Model;

/**
 * Created by Arpad on 3/13/2018.
 */

public class Beacon {

    private String alias;
    private String UUID;
    private int major;
    private int minor;
    private double latitude;
    private double longitude;
    private String instanceId;
    private String room;
    private String level;
    private String roomName;

    public String getUUID() {
        return UUID;
    }

    public String getAlias() {
        return alias;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getRoom() {
        return room;
    }

    public String getLevel() {
        return level;
    }

    public String getRoomName() {
        return roomName;
    }
}

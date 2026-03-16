package org.acme.vehiclerouting.domain;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class Location {
    //    private  String address; TODO
    //    private String place; TODO
    //TODO private int index;
    private double latitude;
    private double longitude;

    @JsonIgnore
    private Map<Location, Long> drivingTimeSeconds;

    @JsonCreator
    public Location(@JsonProperty("latitude") double latitude, @JsonProperty("longitude") double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
    //region GETTERS
    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Map<Location, Long> getDrivingTimeSeconds() {
        return drivingTimeSeconds;
    }

    //TODO public int getIndex(){}
    //endregion

    //region SETTERS
    /**
     * Set the driving time map (in seconds).
     *
     * @param drivingTimeSeconds a map containing driving time from here to other locations
     */
    public void setDrivingTimeSeconds(Map<Location, Long> drivingTimeSeconds) {
        this.drivingTimeSeconds = drivingTimeSeconds;
    }
    //endregion

    //region DRIVINGTIME
    /**
     * Driving time to the given location in seconds.
     *
     * @param location other location
     * @return driving time in seconds
     */
    public long getDrivingTimeTo(Location location) {
        return drivingTimeSeconds.get(location);
    }
    //endregion

    @Override
    public String toString() {
        return latitude + "," + longitude;
    }

}

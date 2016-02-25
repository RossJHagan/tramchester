package com.tramchester.domain;

public class Station implements Location {
    public static String METROLINK_PREFIX = "9400ZZ";

    private final String id;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final boolean isTram;

    public Station(String id, String area, String stopName, double latitude, double longitude, boolean isTram) {
        this.id = id;
        if (isTram) {
            this.name = stopName;
        } else if (area.isEmpty()) {
            this.name = stopName;
        } else {
            this.name = String.format("%s,%s", area, stopName);
        }
        this.latitude = latitude;
        this.longitude = longitude;
        this.isTram = isTram;
    }

    public Station(Station other) {
        this.id = other.id;
        this.name = other.name;
        this.latitude = other.latitude;
        this.longitude = other.longitude;
        this.isTram = other.isTram;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getLatitude() {
        return latitude;
    }

    @Override
    public double getLongitude() {
        return longitude;
    }

    public static String formId(String rawId) {
        if (rawId.startsWith(METROLINK_PREFIX)) {
            // metrolink station ids include platform as final digit, remove to give id of station itself
            int index = rawId.length()-1;
            return rawId.substring(0,index);
        }
        return rawId;
    }

    @Override
    public boolean isTram() {
        return isTram;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Station station = (Station) o;

        return id.equals(station.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Station: [id:%s name:%s]",id, name);
    }
}

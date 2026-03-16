package org.acme.vehiclerouting.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VisitData {
    public String id;
    public String name;
    public double latitude;
    public double longitude;

    // If you DID NOT rename and your JSON still has "long", then do:
    // @JsonProperty("long")
    // public double longitude;
}

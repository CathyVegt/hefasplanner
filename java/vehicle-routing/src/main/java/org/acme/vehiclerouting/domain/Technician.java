package org.acme.vehiclerouting.domain;

import java.util.Map;

public class Technician {
    private String name;
    private Location homeLocation;
    private Map<String, Skill> skills;

    public Technician(String name, Location homeLocation){
        this.name = name;
        this.homeLocation = homeLocation;
    }

    // GETTERS
    public String getName(){
        return name;
    }

    public Location getLocation(){
        return homeLocation;
    }

    @Override
    public String toString(){
        return this.name + "\t" + this.homeLocation;
    }
}

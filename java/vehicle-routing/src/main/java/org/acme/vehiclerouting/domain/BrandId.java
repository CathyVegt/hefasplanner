package org.acme.vehiclerouting.domain;

public record BrandId(String name) {
    @Override
    public String toString(){
        return name;
    }
}

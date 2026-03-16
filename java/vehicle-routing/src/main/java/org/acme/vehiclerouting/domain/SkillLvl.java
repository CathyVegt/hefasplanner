package org.acme.vehiclerouting.domain;

public enum SkillLvl {
    ZERO(0, "0", "Geen ervaring"),
    ONE(1, "1", "Heeft zichzelf eigen gemaakt"),
    TWO(2, "2", "Beschikt over theoretische kennis (certificaat) en basis praktijkervaring"),
    THREE(3, "3", "Goede (theoretische) kennis en ervaring om zelfstandig onderhoud uit te voeren"),
    FOUR(4, "4", "Goede Theoretische kennis en ruime ervaring. Kan alle werkzaamheden uitvoeren"),
    FIVE(5, "5", "Is specialist"),
    K(6, "K", "Is specialist en kan kennis overbrengen");

    private final int number;
    private final String numberString; //TODO might not need it
    private final String description;

    SkillLvl(int number, String numberString, String description){
        this.number = number;
        this.numberString = numberString;
        this.description = description;
    }

    public boolean isAtLeast(SkillLvl skillLvl, SkillLvl required){
        return skillLvl.number >= required.number;
    }

    @Override
    public String toString(){
        return String.format("%s(%d): %s", this.name(), this.number, this.description);
    }

}

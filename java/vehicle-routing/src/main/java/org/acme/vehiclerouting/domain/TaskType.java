package org.acme.vehiclerouting.domain;

public enum TaskType {
    OP, ONDERHOUD, INBEDRIJFSTELLING;


    public static TaskType taskTypefromString(String s){
        return switch (s) {
            case "OP" -> ONDERHOUD;
            case "Onderhoud" -> ONDERHOUD;
            case "ODH" -> ONDERHOUD;
            case "Inbedrijfstelling" -> INBEDRIJFSTELLING;
            case "INB" -> INBEDRIJFSTELLING;
            default -> OP;
        };
    }

}

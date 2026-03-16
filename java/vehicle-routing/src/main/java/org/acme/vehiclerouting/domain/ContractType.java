package org.acme.vehiclerouting.domain;

public enum ContractType {
    ODHOCO("ODH - OCO"),
    ODHOP12("ODH - OP12"),
    ODHOC("ODH - OC"),
    ODHOP11("ODH - OP11"),
    ODHCH("ODH - CH"),
    ODHOP48("ODH - OP4+8"),
    ODHAIOCOP12("ODH - AIOC+OP12"),
    ODHS("ODH - S"),
    BEHBMI("BEH - BMI"),
    BMI("BMI"),
    ODHAIOC("ODH - AIOC"),
    BEHBND("BEH - BND");

    private final String name;
    ContractType(String name){
        this.name = name;
    }

    @Override
    public String toString(){
        return this.name;
    }
}

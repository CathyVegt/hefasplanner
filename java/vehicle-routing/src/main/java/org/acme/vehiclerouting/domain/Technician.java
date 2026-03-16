package org.acme.vehiclerouting.domain;

import java.util.Map;

public class Technician {
    private String name;
    private Location homeLocation;
    private Map<BrandId, Skill> skills;

    public Technician(String name, Location homeLocation, Map<BrandId, Skill> skills){
        this.name = name;
        this.homeLocation = homeLocation;
        this.skills = skills;
    }

    //region GETTERS
    // -------------------------------
    public String getName(){
        return name;
    }

    public Location getLocation(){
        return homeLocation;
    }
    // -------------------------------
    //endregion

    //region SKILLS
    //-------------------------------

    /**TODO match_brand(String brand)
     * check if the string from kennismatrix is in the brand string from the task
     * get keys from skill map
     * check for each
     * @return boolean: true if in the keys from map
     */

    /**TODO minLvl(int lvl)
     * Check if the minlvl requirement is satisfied for that brand
     *
     * @return
     */


    //-------------------------------
    //endregion

    @Override
    public String toString(){
        StringBuilder s = new StringBuilder(this.name + "\n" + "skills: \n");
        for(Map.Entry<BrandId, Skill> skill : skills.entrySet()){
            s.append(skill.getKey()).append(": ").append(skill.getValue());
        }
        return s.toString();
    }
}

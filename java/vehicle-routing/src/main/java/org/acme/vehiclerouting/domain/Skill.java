package org.acme.vehiclerouting.domain;

import java.util.EnumSet;

//TODO set json properties
public record Skill(
        int level,
        EnumSet<TaskType> allowedTaskTypes
){
    public boolean allows(TaskType taskType, int minLvl){
        return level >= minLvl && allowedTaskTypes.contains(taskType);
    }

    @Override
    public String toString(){
        return "level: " + Integer.toString(level) + "\t" + allowedTaskTypes.toString() + "\n";
    }

}
package org.acme.vehiclerouting.domain;

import java.util.EnumSet;

//TODO set json properties
public record Skill(
    int lvl,
    EnumSet<TaskType> allowedTaskTypes
){
    public boolean allows(TaskType taskType, int minLvl ){
        return lvl >= minLvl && allowedTaskTypes.contains(taskType);
    }
}
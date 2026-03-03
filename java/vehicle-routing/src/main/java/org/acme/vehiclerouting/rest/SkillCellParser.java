package org.acme.vehiclerouting.rest;

import org.acme.vehiclerouting.domain.Skill;
import org.acme.vehiclerouting.domain.TaskType;

import java.util.EnumSet;
import java.util.Locale;

public final class SkillCellParser {

    public static Skill parseBrandCapabilityCell(String rawCell) {
        if (rawCell == null) return null;
        String cell = rawCell.trim();
        if (cell.isEmpty()) return null;

        // Allow both comma and semicolon just in case
        String[] parts = cell.split("\\s*,\\s*");
        if (parts.length != 3 && parts.length != 4) {
            throw new IllegalArgumentException("Invalid skill cell (expected 3 or 4 values): '" + rawCell + "'");
        }

        int level = parseIntStrict(parts[0], "level", rawCell);

        EnumSet<TaskType> allowed = EnumSet.noneOf(TaskType.class);

        if (parts.length == 4) {
            // level, OP, ONDERHOUD, INBEDRIJFSTELLING
            if (parseBool01(parts[1], "OP", rawCell)) allowed.add(TaskType.OP);
            if (parseBool01(parts[2], "ONDERHOUD", rawCell)) allowed.add(TaskType.ONDERHOUD);
            if (parseBool01(parts[3], "INBEDRIJFSTELLING", rawCell)) allowed.add(TaskType.INBEDRIJFSTELLING);
        } else {
            // level, ONDERHOUD, INBEDRIJFSTELLING (OP omitted)
            if (parseBool01(parts[1], "ONDERHOUD", rawCell)) allowed.add(TaskType.ONDERHOUD);
            if (parseBool01(parts[2], "INBEDRIJFSTELLING", rawCell)) allowed.add(TaskType.INBEDRIJFSTELLING);
        }

        return new Skill(level, allowed);
    }

    private static boolean parseBool01(String s, String field, String rawCell) {
        String v = s.trim();
        if ("1".equals(v)) return true;
        if ("0".equals(v)) return false;
        throw new IllegalArgumentException("Invalid boolean " + field + " (expected 0/1) in '" + rawCell + "'");
    }

    private static int parseIntStrict(String s, String field, String rawCell) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid int " + field + " in '" + rawCell + "'", e);
        }
    }

    private SkillCellParser() {}
}
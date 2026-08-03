package com.example.util;

public final class SkillFilterUtil {

    private SkillFilterUtil() {}

    /** Splits a comma-separated skills filter into up to 4 individual search terms (padded with ""). */
    public static String[] splitSkillTerms(String skillsCsv) {
        String[] slots = {"", "", "", ""};
        if (skillsCsv == null || skillsCsv.isBlank()) return slots;
        int i = 0;
        for (String term : skillsCsv.split(",")) {
            String t = term.trim();
            if (!t.isEmpty() && i < slots.length) slots[i++] = t;
        }
        return slots;
    }
}

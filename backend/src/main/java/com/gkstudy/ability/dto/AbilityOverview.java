package com.gkstudy.ability.dto;

import com.gkstudy.ability.model.AbilityProfile;

import java.util.List;

public class AbilityOverview {
    private final int evaluatedCount;
    private final int totalCount;
    private final int coveragePercent;
    private final List<AbilityProfile> abilities;

    public AbilityOverview(int evaluatedCount, int totalCount, List<AbilityProfile> abilities) {
        this.evaluatedCount = evaluatedCount; this.totalCount = totalCount;
        this.coveragePercent = totalCount == 0 ? 0 : (int) Math.round(evaluatedCount * 100.0 / totalCount);
        this.abilities = abilities;
    }

    public int getEvaluatedCount() { return evaluatedCount; }
    public int getTotalCount() { return totalCount; }
    public int getCoveragePercent() { return coveragePercent; }
    public List<AbilityProfile> getAbilities() { return abilities; }
}

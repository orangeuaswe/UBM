package com.UBM.BMW.domain;

import java.util.List;
public record KnownIssue(Long id, String slug, String scopeType, String scopeValue, String category, String issue, String severity, Integer mileageMin, Integer mileageMax, Integer costMinUSD, Integer costMaxUSD, boolean totalLossRisk, boolean inspectionRequired, String affectedVariants, String notes, List<String> sources)
{
    public int severityRisk()
    {
        return switch(severity == null ? " ":severity)
        {
            case "critical" -> 4;
            case "high" -> 3;
            case "medium" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }
    public boolean hasUnverifiedSources()
    {
        return sources != null && sources.stream().anyMatch(s -> s != null && s.startsWith("UNVERIFIED"));
    }
}

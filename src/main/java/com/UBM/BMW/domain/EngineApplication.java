package com.UBM.BMW.domain;

public record EngineApplication(Long id, String chassisCode, String modelName, String engineCode, Boolean lci, Integer yearFrom, Integer yearTo, String market, String source, String notes)
{
    public boolean matchYear(Integer year)
    {
        if (year == null)
        {
            return true;
        }
        return (yearFrom == null || year >= yearFrom) && (yearTo == null || year <= yearTo);
    }
}

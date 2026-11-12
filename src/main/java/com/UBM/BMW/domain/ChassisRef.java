package com.UBM.BMW.domain;

public record ChassisRef(String code, String modelLine, int startYear, int endYear, Integer lciYear, String notes)
{
    public boolean containYear(int year)
    {
        return year >= startYear && year <= endYear;
    }
    public boolean isLCI (int year)
    {
        return lciYear != null && year >=lciYear;
    }
}

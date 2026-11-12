package com.UBM.BMW.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record Vehicle(Long id, String vin, String make, String model, Integer modelYear, String chassisCode, String engineCode, String trim, String body, String tranny, String plant, LocalDate prodDate, String engineResolutionSource, String engineConfidence, String engineHint, BigDecimal displacement, Integer cylinders, OffsetDateTime recallsFetchedAt)
{
    public boolean isDecoded()
    {
        return make != null;
    }
}

package com.example.petcare.health;

import java.util.List;

public record HealthRecordSummary(
        List<VaccineRecord> vaccines,
        List<DewormingRecord> dewormingRecords,
        List<WeightRecord> weights
) {
}

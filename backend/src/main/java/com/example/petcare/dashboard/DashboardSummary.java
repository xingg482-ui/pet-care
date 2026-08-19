package com.example.petcare.dashboard;

public record DashboardSummary(
        long customerCount,
        long petCount,
        long serviceItemCount,
        long pendingOrderCount,
        long todayAppointmentCount,
        long boardingAvailableLocationCount
) {
}

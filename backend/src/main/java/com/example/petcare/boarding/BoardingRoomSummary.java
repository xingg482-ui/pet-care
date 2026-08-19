package com.example.petcare.boarding;

public record BoardingRoomSummary(
        long todayPendingCheckIn,
        long todayPendingCheckOut,
        long todayOccupiedCapacity,
        long availableCapacity
) {
}

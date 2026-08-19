package com.example.petcare.boarding;

import java.util.List;

public record BoardingRoomStatusView(
        String startDate,
        String endDate,
        int days,
        List<String> dates,
        BoardingRoomSummary summary,
        List<BoardingRoomRow> rows
) {
}

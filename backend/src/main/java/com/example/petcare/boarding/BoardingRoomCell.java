package com.example.petcare.boarding;

public record BoardingRoomCell(
        String date,
        String status,
        BoardingOrderView order
) {
}

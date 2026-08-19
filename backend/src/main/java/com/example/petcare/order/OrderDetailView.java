package com.example.petcare.order;

import com.example.petcare.customer.Customer;
import com.example.petcare.pet.Pet;

import java.util.List;

public record OrderDetailView(
        OrderListView order,
        Customer customer,
        Pet pet,
        List<ServiceOrderItem> items,
        List<PaymentRecord> paymentRecords,
        List<OrderStatusLog> statusLogs
) {
}

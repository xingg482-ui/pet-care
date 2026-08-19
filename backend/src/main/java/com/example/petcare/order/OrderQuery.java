package com.example.petcare.order;

public class OrderQuery {
    private String orderNo;
    private String customerName;
    private String petName;
    private String status;
    private String paymentStatus;
    private String appointmentStart;
    private String appointmentEnd;
    private long page = 1;
    private long pageSize = 10;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getAppointmentStart() { return appointmentStart; }
    public void setAppointmentStart(String appointmentStart) { this.appointmentStart = appointmentStart; }
    public String getAppointmentEnd() { return appointmentEnd; }
    public void setAppointmentEnd(String appointmentEnd) { this.appointmentEnd = appointmentEnd; }
    public long getPage() { return page; }
    public void setPage(long page) { this.page = Math.max(page, 1); }
    public long getPageSize() { return pageSize; }
    public void setPageSize(long pageSize) { this.pageSize = pageSize <= 0 ? 10 : Math.min(pageSize, 100); }
}

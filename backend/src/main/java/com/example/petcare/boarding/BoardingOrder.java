package com.example.petcare.boarding;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

@TableName("boarding_order")
public class BoardingOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String boardingNo;
    private Long customerId;
    private Long petId;
    private Long locationId;
    private String plannedCheckInTime;
    private String plannedCheckOutTime;
    private String actualCheckInTime;
    private String actualCheckOutTime;
    private String status;
    private BigDecimal unitPrice;
    private BigDecimal unitCost;
    private Integer chargeDays;
    private BigDecimal totalAmount;
    private BigDecimal totalCost;
    private BigDecimal totalProfit;
    private String paymentStatus;
    private BigDecimal paidAmount;
    private String paidAt;
    private String paymentMethod;
    private String paymentNo;
    private String remark;
    private String createdAt;
    private String updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBoardingNo() {
        return boardingNo;
    }

    public void setBoardingNo(String boardingNo) {
        this.boardingNo = boardingNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getPlannedCheckInTime() {
        return plannedCheckInTime;
    }

    public void setPlannedCheckInTime(String plannedCheckInTime) {
        this.plannedCheckInTime = plannedCheckInTime;
    }

    public String getPlannedCheckOutTime() {
        return plannedCheckOutTime;
    }

    public void setPlannedCheckOutTime(String plannedCheckOutTime) {
        this.plannedCheckOutTime = plannedCheckOutTime;
    }

    public String getActualCheckInTime() {
        return actualCheckInTime;
    }

    public void setActualCheckInTime(String actualCheckInTime) {
        this.actualCheckInTime = actualCheckInTime;
    }

    public String getActualCheckOutTime() {
        return actualCheckOutTime;
    }

    public void setActualCheckOutTime(String actualCheckOutTime) {
        this.actualCheckOutTime = actualCheckOutTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public Integer getChargeDays() {
        return chargeDays;
    }

    public void setChargeDays(Integer chargeDays) {
        this.chargeDays = chargeDays;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(BigDecimal totalProfit) {
        this.totalProfit = totalProfit;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(String paidAt) {
        this.paidAt = paidAt;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}

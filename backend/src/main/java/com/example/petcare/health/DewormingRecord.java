package com.example.petcare.health;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("deworming_record")
public class DewormingRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long petId;
    private String dewormingType;
    private String medicineName;
    private String dewormingDate;
    private String nextDewormingDate;
    private String remark;
    private String createdAt;
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public String getDewormingType() { return dewormingType; }
    public void setDewormingType(String dewormingType) { this.dewormingType = dewormingType; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getDewormingDate() { return dewormingDate; }
    public void setDewormingDate(String dewormingDate) { this.dewormingDate = dewormingDate; }
    public String getNextDewormingDate() { return nextDewormingDate; }
    public void setNextDewormingDate(String nextDewormingDate) { this.nextDewormingDate = nextDewormingDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}

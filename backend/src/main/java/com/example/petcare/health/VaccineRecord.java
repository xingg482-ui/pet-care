package com.example.petcare.health;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("vaccine_record")
public class VaccineRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long petId;
    private String vaccineName;
    private String vaccinationDate;
    private String institution;
    private String nextVaccinationDate;
    private String remark;
    private String createdAt;
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public String getVaccineName() { return vaccineName; }
    public void setVaccineName(String vaccineName) { this.vaccineName = vaccineName; }
    public String getVaccinationDate() { return vaccinationDate; }
    public void setVaccinationDate(String vaccinationDate) { this.vaccinationDate = vaccinationDate; }
    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
    public String getNextVaccinationDate() { return nextVaccinationDate; }
    public void setNextVaccinationDate(String nextVaccinationDate) { this.nextVaccinationDate = nextVaccinationDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}

package com.example.petcare.health;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcare.pet.Pet;
import com.example.petcare.pet.PetMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class HealthRecordService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PetMapper petMapper;
    private final VaccineRecordMapper vaccineRecordMapper;
    private final DewormingRecordMapper dewormingRecordMapper;
    private final WeightRecordMapper weightRecordMapper;

    public HealthRecordService(
            PetMapper petMapper,
            VaccineRecordMapper vaccineRecordMapper,
            DewormingRecordMapper dewormingRecordMapper,
            WeightRecordMapper weightRecordMapper
    ) {
        this.petMapper = petMapper;
        this.vaccineRecordMapper = vaccineRecordMapper;
        this.dewormingRecordMapper = dewormingRecordMapper;
        this.weightRecordMapper = weightRecordMapper;
    }

    public HealthRecordSummary summary(Long petId) {
        ensurePetExists(petId);
        return new HealthRecordSummary(
                vaccineRecordMapper.selectList(new LambdaQueryWrapper<VaccineRecord>()
                        .eq(VaccineRecord::getPetId, petId)
                        .orderByDesc(VaccineRecord::getVaccinationDate)),
                dewormingRecordMapper.selectList(new LambdaQueryWrapper<DewormingRecord>()
                        .eq(DewormingRecord::getPetId, petId)
                        .orderByDesc(DewormingRecord::getDewormingDate)),
                weightRecordMapper.selectList(new LambdaQueryWrapper<WeightRecord>()
                        .eq(WeightRecord::getPetId, petId)
                        .orderByDesc(WeightRecord::getRecordDate))
        );
    }

    public VaccineRecord createVaccine(Long petId, VaccineRecordRequest request) {
        ensurePetExists(petId);
        VaccineRecord record = new VaccineRecord();
        record.setPetId(petId);
        applyVaccine(record, request);
        record.setCreatedAt(now());
        record.setUpdatedAt(now());
        vaccineRecordMapper.insert(record);
        return vaccineRecordMapper.selectById(record.getId());
    }

    public VaccineRecord updateVaccine(Long petId, Long id, VaccineRecordRequest request) {
        ensurePetExists(petId);
        VaccineRecord record = getVaccineOrThrow(petId, id);
        applyVaccine(record, request);
        record.setUpdatedAt(now());
        vaccineRecordMapper.updateById(record);
        return vaccineRecordMapper.selectById(id);
    }

    public void deleteVaccine(Long petId, Long id) {
        getVaccineOrThrow(petId, id);
        vaccineRecordMapper.deleteById(id);
    }

    public DewormingRecord createDeworming(Long petId, DewormingRecordRequest request) {
        ensurePetExists(petId);
        validateDewormingType(request.dewormingType());
        DewormingRecord record = new DewormingRecord();
        record.setPetId(petId);
        applyDeworming(record, request);
        record.setCreatedAt(now());
        record.setUpdatedAt(now());
        dewormingRecordMapper.insert(record);
        return dewormingRecordMapper.selectById(record.getId());
    }

    public DewormingRecord updateDeworming(Long petId, Long id, DewormingRecordRequest request) {
        ensurePetExists(petId);
        validateDewormingType(request.dewormingType());
        DewormingRecord record = getDewormingOrThrow(petId, id);
        applyDeworming(record, request);
        record.setUpdatedAt(now());
        dewormingRecordMapper.updateById(record);
        return dewormingRecordMapper.selectById(id);
    }

    public void deleteDeworming(Long petId, Long id) {
        getDewormingOrThrow(petId, id);
        dewormingRecordMapper.deleteById(id);
    }

    public WeightRecord createWeight(Long petId, WeightRecordRequest request) {
        ensurePetExists(petId);
        WeightRecord record = new WeightRecord();
        record.setPetId(petId);
        applyWeight(record, request);
        record.setCreatedAt(now());
        record.setUpdatedAt(now());
        weightRecordMapper.insert(record);
        return weightRecordMapper.selectById(record.getId());
    }

    public WeightRecord updateWeight(Long petId, Long id, WeightRecordRequest request) {
        ensurePetExists(petId);
        WeightRecord record = getWeightOrThrow(petId, id);
        applyWeight(record, request);
        record.setUpdatedAt(now());
        weightRecordMapper.updateById(record);
        return weightRecordMapper.selectById(id);
    }

    public void deleteWeight(Long petId, Long id) {
        getWeightOrThrow(petId, id);
        weightRecordMapper.deleteById(id);
    }

    private void ensurePetExists(Long petId) {
        Pet pet = petMapper.selectById(petId);
        if (pet == null) {
            throw new IllegalArgumentException("宠物不存在");
        }
    }

    private VaccineRecord getVaccineOrThrow(Long petId, Long id) {
        VaccineRecord record = vaccineRecordMapper.selectById(id);
        if (record == null || !petId.equals(record.getPetId())) {
            throw new IllegalArgumentException("疫苗记录不存在");
        }
        return record;
    }

    private DewormingRecord getDewormingOrThrow(Long petId, Long id) {
        DewormingRecord record = dewormingRecordMapper.selectById(id);
        if (record == null || !petId.equals(record.getPetId())) {
            throw new IllegalArgumentException("驱虫记录不存在");
        }
        return record;
    }

    private WeightRecord getWeightOrThrow(Long petId, Long id) {
        WeightRecord record = weightRecordMapper.selectById(id);
        if (record == null || !petId.equals(record.getPetId())) {
            throw new IllegalArgumentException("体重记录不存在");
        }
        return record;
    }

    private void applyVaccine(VaccineRecord record, VaccineRecordRequest request) {
        record.setVaccineName(request.vaccineName());
        record.setVaccinationDate(request.vaccinationDate());
        record.setInstitution(request.institution());
        record.setNextVaccinationDate(request.nextVaccinationDate());
        record.setRemark(request.remark());
    }

    private void applyDeworming(DewormingRecord record, DewormingRecordRequest request) {
        record.setDewormingType(request.dewormingType());
        record.setMedicineName(request.medicineName());
        record.setDewormingDate(request.dewormingDate());
        record.setNextDewormingDate(request.nextDewormingDate());
        record.setRemark(request.remark());
    }

    private void applyWeight(WeightRecord record, WeightRecordRequest request) {
        record.setRecordDate(request.recordDate());
        record.setWeight(request.weight());
        record.setRemark(request.remark());
    }

    private void validateDewormingType(String type) {
        if (!"体内".equals(type) && !"体外".equals(type)) {
            throw new IllegalArgumentException("驱虫类型必须为体内或体外");
        }
    }

    private String now() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}

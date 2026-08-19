package com.example.petcare.pet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.petcare.boarding.BoardingOrder;
import com.example.petcare.boarding.BoardingOrderMapper;
import com.example.petcare.common.PageResult;
import com.example.petcare.customer.Customer;
import com.example.petcare.customer.CustomerMapper;
import com.example.petcare.account.AccountPrincipal;
import com.example.petcare.account.AccountService;
import com.example.petcare.health.DewormingRecord;
import com.example.petcare.health.DewormingRecordMapper;
import com.example.petcare.health.VaccineRecord;
import com.example.petcare.health.VaccineRecordMapper;
import com.example.petcare.health.WeightRecord;
import com.example.petcare.health.WeightRecordMapper;
import com.example.petcare.order.ServiceOrder;
import com.example.petcare.order.ServiceOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PetService {

    private static final String ENABLED = "ENABLED";
    private static final String DISABLED = "DISABLED";
    private static final String UNKNOWN = "UNKNOWN";
    private static final String UPLOADED = "UPLOADED";
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path AVATAR_DIRECTORY = Path.of("uploads", "pet-avatars");

    private final PetMapper petMapper;
    private final CustomerMapper customerMapper;
    private final AccountService accountService;
    private final ServiceOrderMapper serviceOrderMapper;
    private final BoardingOrderMapper boardingOrderMapper;
    private final VaccineRecordMapper vaccineRecordMapper;
    private final DewormingRecordMapper dewormingRecordMapper;
    private final WeightRecordMapper weightRecordMapper;

    public PetService(
            PetMapper petMapper,
            CustomerMapper customerMapper,
            AccountService accountService,
            ServiceOrderMapper serviceOrderMapper,
            BoardingOrderMapper boardingOrderMapper,
            VaccineRecordMapper vaccineRecordMapper,
            DewormingRecordMapper dewormingRecordMapper,
            WeightRecordMapper weightRecordMapper
    ) {
        this.petMapper = petMapper;
        this.customerMapper = customerMapper;
        this.accountService = accountService;
        this.serviceOrderMapper = serviceOrderMapper;
        this.boardingOrderMapper = boardingOrderMapper;
        this.vaccineRecordMapper = vaccineRecordMapper;
        this.dewormingRecordMapper = dewormingRecordMapper;
        this.weightRecordMapper = weightRecordMapper;
    }

    public PageResult<PetView> list(PetQuery query) {
        LambdaQueryWrapper<Pet> wrapper = buildQuery(query).orderByDesc(Pet::getCreatedAt);
        Page<Pet> page = petMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        Map<Long, Customer> customers = loadCustomers(page.getRecords().stream()
                .map(Pet::getCustomerId)
                .collect(Collectors.toSet()));
        return new PageResult<>(
                page.getRecords().stream()
                        .map(pet -> PetView.from(pet, customerName(customers.get(pet.getCustomerId()))))
                        .toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    public PetView getViewByIdOrThrow(Long id) {
        Pet pet = getByIdOrThrow(id);
        Customer customer = customerMapper.selectById(pet.getCustomerId());
        return PetView.from(pet, customerName(customer));
    }

    public PageResult<PetView> listByCustomer(Long customerId, boolean onlyEnabled) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        PetQuery query = new PetQuery();
        query.setCustomerId(customerId);
        query.setPageSize(100);
        if (onlyEnabled) {
            query.setStatus(ENABLED);
        }
        return list(query);
    }

    public PageResult<PetView> listMyPets(String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        return listByCustomer(principal.customerId(), false);
    }

    public PetView createMyPet(PetRequest request, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        return create(copyForCustomer(request, principal.customerId()));
    }

    public PetView updateMyPet(Long id, PetRequest request, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        Pet pet = getByIdOrThrow(id);
        if (!principal.customerId().equals(pet.getCustomerId())) {
            throw new IllegalArgumentException("不能编辑其他客户的宠物");
        }
        return update(id, copyForCustomer(request, principal.customerId()));
    }

    public PetView uploadMyPetAvatar(Long id, MultipartFile file, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        Pet pet = getByIdOrThrow(id);
        if (!principal.customerId().equals(pet.getCustomerId())) {
            throw new IllegalArgumentException("不能编辑其他客户的宠物");
        }
        return uploadAvatar(id, file);
    }

    public PetView removeMyPetAvatar(Long id, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        Pet pet = getByIdOrThrow(id);
        if (!principal.customerId().equals(pet.getCustomerId())) {
            throw new IllegalArgumentException("不能编辑其他客户的宠物");
        }
        return removeAvatar(id);
    }

    public PetView updateMyPetStatus(Long id, String status, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        Pet pet = getByIdOrThrow(id);
        if (!principal.customerId().equals(pet.getCustomerId())) {
            throw new IllegalArgumentException("不能编辑其他客户的宠物");
        }
        return updateStatus(id, status);
    }

    public PetView create(PetRequest request) {
        ensureCustomerEnabled(request.customerId());
        Pet pet = new Pet();
        applyRequest(pet, request);
        pet.setStatus(ENABLED);
        pet.setCreatedAt(now());
        pet.setUpdatedAt(now());
        petMapper.insert(pet);
        return getViewByIdOrThrow(pet.getId());
    }

    public PetView update(Long id, PetRequest request) {
        Pet pet = getByIdOrThrow(id);
        ensureCustomerEnabled(request.customerId());
        applyRequest(pet, request);
        pet.setUpdatedAt(now());
        petMapper.updateById(pet);
        return getViewByIdOrThrow(id);
    }

    public PetView updateStatus(Long id, String status) {
        getByIdOrThrow(id);
        if (!ENABLED.equals(status) && !DISABLED.equals(status)) {
            throw new IllegalArgumentException("宠物状态不合法");
        }
        petMapper.update(new LambdaUpdateWrapper<Pet>()
                .eq(Pet::getId, id)
                .set(Pet::getStatus, status)
                .set(Pet::getUpdatedAt, now()));
        return getViewByIdOrThrow(id);
    }

    public PetView uploadAvatar(Long id, MultipartFile file) {
        Pet pet = getByIdOrThrow(id);
        validateAvatar(file);

        String extension = extension(file.getOriginalFilename(), file.getContentType());
        String filename = "pet-" + id + "-" + UUID.randomUUID() + "." + extension;
        Path target = AVATAR_DIRECTORY.toAbsolutePath().normalize().resolve(filename).normalize();
        Path avatarRoot = AVATAR_DIRECTORY.toAbsolutePath().normalize();
        if (!target.startsWith(avatarRoot)) {
            throw new IllegalArgumentException("头像文件路径不合法");
        }

        try {
            Files.createDirectories(avatarRoot);
            file.transferTo(target);
            deleteLocalAvatar(pet.getAvatarUrl());
        } catch (IOException exception) {
            throw new IllegalArgumentException("头像上传失败，请稍后重试");
        }

        pet.setAvatarUrl("/uploads/pet-avatars/" + filename);
        pet.setAvatarSource(UPLOADED);
        pet.setUpdatedAt(now());
        petMapper.updateById(pet);
        return getViewByIdOrThrow(id);
    }

    public PetView removeAvatar(Long id) {
        Pet pet = getByIdOrThrow(id);
        deleteLocalAvatar(pet.getAvatarUrl());
        petMapper.update(new LambdaUpdateWrapper<Pet>()
                .eq(Pet::getId, id)
                .set(Pet::getAvatarUrl, null)
                .set(Pet::getAvatarSource, null)
                .set(Pet::getUpdatedAt, now()));
        return getViewByIdOrThrow(id);
    }

    public void delete(Long id) {
        Pet pet = getByIdOrThrow(id);
        if (!DISABLED.equals(pet.getStatus())) {
            throw new IllegalArgumentException("只有已停用的宠物可以删除");
        }
        ensurePetNotUsed(id);
        deleteLocalAvatar(pet.getAvatarUrl());
        petMapper.deleteById(id);
    }

    private LambdaQueryWrapper<Pet> buildQuery(PetQuery query) {
        return new LambdaQueryWrapper<Pet>()
                .like(StringUtils.hasText(query.getName()), Pet::getName, query.getName())
                .eq(query.getCustomerId() != null, Pet::getCustomerId, query.getCustomerId())
                .like(StringUtils.hasText(query.getSpecies()), Pet::getSpecies, query.getSpecies())
                .eq(StringUtils.hasText(query.getStatus()), Pet::getStatus, query.getStatus());
    }

    private Pet getByIdOrThrow(Long id) {
        Pet pet = petMapper.selectById(id);
        if (pet == null) {
            throw new IllegalArgumentException("宠物不存在");
        }
        return pet;
    }

    private void ensurePetNotUsed(Long id) {
        Long serviceOrderCount = serviceOrderMapper.selectCount(new LambdaQueryWrapper<ServiceOrder>()
                .eq(ServiceOrder::getPetId, id));
        if (serviceOrderCount != null && serviceOrderCount > 0) {
            throw new IllegalArgumentException("该宠物已有订单记录，不能删除");
        }
        Long boardingOrderCount = boardingOrderMapper.selectCount(new LambdaQueryWrapper<BoardingOrder>()
                .eq(BoardingOrder::getPetId, id));
        if (boardingOrderCount != null && boardingOrderCount > 0) {
            throw new IllegalArgumentException("该宠物已有托管记录，不能删除");
        }
        Long vaccineCount = vaccineRecordMapper.selectCount(new LambdaQueryWrapper<VaccineRecord>()
                .eq(VaccineRecord::getPetId, id));
        Long dewormingCount = dewormingRecordMapper.selectCount(new LambdaQueryWrapper<DewormingRecord>()
                .eq(DewormingRecord::getPetId, id));
        Long weightCount = weightRecordMapper.selectCount(new LambdaQueryWrapper<WeightRecord>()
                .eq(WeightRecord::getPetId, id));
        long healthRecordCount = (vaccineCount == null ? 0 : vaccineCount)
                + (dewormingCount == null ? 0 : dewormingCount)
                + (weightCount == null ? 0 : weightCount);
        if (healthRecordCount > 0) {
            throw new IllegalArgumentException("该宠物已有健康记录，不能删除");
        }
    }

    private void ensureCustomerEnabled(Long customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        if (!ENABLED.equals(customer.getStatus())) {
            throw new IllegalArgumentException("停用客户不能关联新宠物");
        }
    }

    private void applyRequest(Pet pet, PetRequest request) {
        pet.setCustomerId(request.customerId());
        pet.setName(request.name());
        pet.setSpecies(request.species());
        pet.setBreed(request.breed());
        pet.setGender(StringUtils.hasText(request.gender()) ? request.gender() : UNKNOWN);
        pet.setBirthday(request.birthday());
        pet.setWeight(request.weight());
        pet.setSterilized(request.sterilized());
        pet.setColor(request.color());
        pet.setMicrochipNo(request.microchipNo());
        pet.setAllergies(request.allergies());
        pet.setMedicalHistory(request.medicalHistory());
        pet.setDietPreference(request.dietPreference());
        pet.setBehaviorNotes(request.behaviorNotes());
        pet.setExerciseLevel(request.exerciseLevel());
        pet.setCareNotes(request.careNotes());
        pet.setRemark(request.remark());
    }

    private PetRequest copyForCustomer(PetRequest request, Long customerId) {
        return new PetRequest(
                customerId,
                request.name(),
                request.species(),
                request.breed(),
                request.gender(),
                request.birthday(),
                request.weight(),
                request.sterilized(),
                request.color(),
                request.microchipNo(),
                request.allergies(),
                request.medicalHistory(),
                request.dietPreference(),
                request.behaviorNotes(),
                request.exerciseLevel(),
                request.careNotes(),
                request.remark()
        );
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择头像文件");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("头像文件不能超过2MB");
        }
        String contentType = file.getContentType();
        if (!Set.of("image/jpeg", "image/png", "image/webp").contains(contentType)) {
            throw new IllegalArgumentException("头像仅支持 jpg、png、webp 格式");
        }
    }

    private String extension(String originalFilename, String contentType) {
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (Set.of("jpg", "jpeg", "png", "webp").contains(ext)) {
                return ext;
            }
        }
        if ("image/png".equals(contentType)) {
            return "png";
        }
        if ("image/webp".equals(contentType)) {
            return "webp";
        }
        return "jpg";
    }

    private void deleteLocalAvatar(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl) || !avatarUrl.startsWith("/uploads/pet-avatars/")) {
            return;
        }
        Path avatarRoot = AVATAR_DIRECTORY.toAbsolutePath().normalize();
        Path target = avatarRoot.resolve(avatarUrl.substring("/uploads/pet-avatars/".length())).normalize();
        if (!target.startsWith(avatarRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }

    private Map<Long, Customer> loadCustomers(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return customerMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
    }

    private String customerName(Customer customer) {
        return customer == null ? "-" : customer.getName();
    }

    private String now() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}

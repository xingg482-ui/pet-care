package com.example.petcare.petavatar;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PetAvatarLibraryService {

    private static final String ENABLED = "ENABLED";
    private static final String DISABLED = "DISABLED";
    private static final String CUSTOM = "CUSTOM";
    private static final String SYSTEM = "SYSTEM";
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path AVATAR_DIRECTORY = Path.of("uploads", "pet-avatar-library");

    private final PetAvatarLibraryMapper avatarMapper;

    public PetAvatarLibraryService(PetAvatarLibraryMapper avatarMapper) {
        this.avatarMapper = avatarMapper;
    }

    public List<PetAvatarLibraryView> list(PetAvatarLibraryQuery query) {
        return avatarMapper.selectList(buildQuery(query)
                        .orderByAsc(PetAvatarLibrary::getSortOrder)
                        .orderByAsc(PetAvatarLibrary::getId))
                .stream()
                .map(PetAvatarLibraryView::from)
                .toList();
    }

    public PetAvatarLibraryView match(String species, String breed) {
        String text = ((breed == null ? "" : breed) + " " + (species == null ? "" : species)).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return avatarMapper.selectList(new LambdaQueryWrapper<PetAvatarLibrary>()
                        .eq(PetAvatarLibrary::getStatus, ENABLED)
                        .orderByAsc(PetAvatarLibrary::getSortOrder)
                        .orderByAsc(PetAvatarLibrary::getId))
                .stream()
                .filter(item -> matches(item, text))
                .findFirst()
                .map(PetAvatarLibraryView::from)
                .orElse(null);
    }

    public PetAvatarLibraryView create(PetAvatarLibraryRequest request) {
        PetAvatarLibrary avatar = new PetAvatarLibrary();
        applyRequest(avatar, request);
        avatar.setSourceType(StringUtils.hasText(request.avatarUrl()) && request.avatarUrl().startsWith("/pet-avatars/system/")
                ? SYSTEM
                : CUSTOM);
        avatar.setStatus(ENABLED);
        avatar.setCreatedAt(now());
        avatar.setUpdatedAt(now());
        avatarMapper.insert(avatar);
        return PetAvatarLibraryView.from(avatar);
    }

    public PetAvatarLibraryView update(Long id, PetAvatarLibraryRequest request) {
        PetAvatarLibrary avatar = getByIdOrThrow(id);
        applyRequest(avatar, request);
        if (StringUtils.hasText(request.avatarUrl())) {
            avatar.setSourceType(request.avatarUrl().startsWith("/pet-avatars/system/") ? SYSTEM : CUSTOM);
        }
        avatar.setUpdatedAt(now());
        avatarMapper.updateById(avatar);
        return PetAvatarLibraryView.from(avatar);
    }

    public PetAvatarLibraryView updateStatus(Long id, String status) {
        getByIdOrThrow(id);
        if (!ENABLED.equals(status) && !DISABLED.equals(status)) {
            throw new IllegalArgumentException("形象库状态不合法");
        }
        avatarMapper.update(new LambdaUpdateWrapper<PetAvatarLibrary>()
                .eq(PetAvatarLibrary::getId, id)
                .set(PetAvatarLibrary::getStatus, status)
                .set(PetAvatarLibrary::getUpdatedAt, now()));
        return PetAvatarLibraryView.from(getByIdOrThrow(id));
    }

    public PetAvatarLibraryView uploadAvatar(Long id, MultipartFile file) {
        PetAvatarLibrary avatar = getByIdOrThrow(id);
        validateAvatar(file);

        String extension = extension(file.getOriginalFilename(), file.getContentType());
        String filename = "library-" + id + "-" + UUID.randomUUID() + "." + extension;
        Path avatarRoot = AVATAR_DIRECTORY.toAbsolutePath().normalize();
        Path target = avatarRoot.resolve(filename).normalize();
        if (!target.startsWith(avatarRoot)) {
            throw new IllegalArgumentException("头像文件路径不合法");
        }

        try {
            Files.createDirectories(avatarRoot);
            file.transferTo(target);
            deleteLocalAvatar(avatar.getAvatarUrl());
        } catch (IOException exception) {
            throw new IllegalArgumentException("头像上传失败，请稍后重试");
        }

        avatar.setAvatarUrl("/uploads/pet-avatar-library/" + filename);
        avatar.setSourceType(CUSTOM);
        avatar.setUpdatedAt(now());
        avatarMapper.updateById(avatar);
        return PetAvatarLibraryView.from(avatar);
    }

    private LambdaQueryWrapper<PetAvatarLibrary> buildQuery(PetAvatarLibraryQuery query) {
        String keyword = query.getKeyword();
        return new LambdaQueryWrapper<PetAvatarLibrary>()
                .eq(StringUtils.hasText(query.getSpecies()), PetAvatarLibrary::getSpecies, query.getSpecies())
                .eq(StringUtils.hasText(query.getStatus()), PetAvatarLibrary::getStatus, query.getStatus())
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(PetAvatarLibrary::getBreed, keyword)
                        .or()
                        .like(PetAvatarLibrary::getKeywords, keyword));
    }

    private boolean matches(PetAvatarLibrary avatar, String text) {
        if (StringUtils.hasText(avatar.getBreed()) && text.contains(avatar.getBreed().toLowerCase(Locale.ROOT))) {
            return true;
        }
        if (!StringUtils.hasText(avatar.getKeywords())) {
            return false;
        }
        for (String keyword : avatar.getKeywords().split("[,，]")) {
            if (StringUtils.hasText(keyword) && text.contains(keyword.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private void applyRequest(PetAvatarLibrary avatar, PetAvatarLibraryRequest request) {
        avatar.setSpecies(request.species());
        avatar.setBreed(request.breed());
        avatar.setKeywords(request.keywords());
        avatar.setAvatarUrl(request.avatarUrl());
        avatar.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        avatar.setRemark(request.remark());
    }

    private PetAvatarLibrary getByIdOrThrow(Long id) {
        PetAvatarLibrary avatar = avatarMapper.selectById(id);
        if (avatar == null) {
            throw new IllegalArgumentException("形象库条目不存在");
        }
        return avatar;
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择头像文件");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("头像文件不能超过2MB");
        }
        if (!Set.of("image/jpeg", "image/png", "image/webp").contains(file.getContentType())) {
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
        if (!StringUtils.hasText(avatarUrl) || !avatarUrl.startsWith("/uploads/pet-avatar-library/")) {
            return;
        }
        Path avatarRoot = AVATAR_DIRECTORY.toAbsolutePath().normalize();
        Path target = avatarRoot.resolve(avatarUrl.substring("/uploads/pet-avatar-library/".length())).normalize();
        if (!target.startsWith(avatarRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }

    private String now() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}

package com.example.petcare.serviceitem;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.petcare.common.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ServiceItemService {

    private static final String ENABLED = "ENABLED";
    private static final String DISABLED = "DISABLED";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ServiceItemMapper serviceItemMapper;

    public ServiceItemService(ServiceItemMapper serviceItemMapper) {
        this.serviceItemMapper = serviceItemMapper;
    }

    public PageResult<ServiceItem> list(ServiceItemQuery query) {
        LambdaQueryWrapper<ServiceItem> wrapper = new LambdaQueryWrapper<ServiceItem>()
                .like(StringUtils.hasText(query.getName()), ServiceItem::getName, query.getName())
                .like(StringUtils.hasText(query.getCategory()), ServiceItem::getCategory, query.getCategory())
                .eq(StringUtils.hasText(query.getStatus()), ServiceItem::getStatus, query.getStatus())
                .orderByDesc(ServiceItem::getUpdatedAt);
        Page<ServiceItem> page = serviceItemMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<ServiceItem> listEnabled() {
        ServiceItemQuery query = new ServiceItemQuery();
        query.setStatus(ENABLED);
        query.setPageSize(100);
        return list(query);
    }

    public ServiceItem getByIdOrThrow(Long id) {
        ServiceItem item = serviceItemMapper.selectById(id);
        if (item == null) {
            throw new IllegalArgumentException("服务项目不存在");
        }
        return item;
    }

    public ServiceItem create(ServiceItemRequest request) {
        ServiceItem item = new ServiceItem();
        applyRequest(item, request);
        item.setStatus(ENABLED);
        item.setCreatedAt(now());
        item.setUpdatedAt(now());
        serviceItemMapper.insert(item);
        return getByIdOrThrow(item.getId());
    }

    public ServiceItem update(Long id, ServiceItemRequest request) {
        ServiceItem item = getByIdOrThrow(id);
        applyRequest(item, request);
        item.setUpdatedAt(now());
        serviceItemMapper.updateById(item);
        return getByIdOrThrow(id);
    }

    public ServiceItem updateStatus(Long id, String status) {
        getByIdOrThrow(id);
        if (!ENABLED.equals(status) && !DISABLED.equals(status)) {
            throw new IllegalArgumentException("服务项目状态不合法");
        }
        serviceItemMapper.update(new LambdaUpdateWrapper<ServiceItem>()
                .eq(ServiceItem::getId, id)
                .set(ServiceItem::getStatus, status)
                .set(ServiceItem::getUpdatedAt, now()));
        return getByIdOrThrow(id);
    }

    private void applyRequest(ServiceItem item, ServiceItemRequest request) {
        item.setName(request.name());
        item.setCategory(request.category());
        item.setPrice(request.price());
        item.setDurationMinutes(request.durationMinutes());
        item.setDescription(request.description());
    }

    private String now() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}

package com.example.petcare.serviceitem;

import com.example.petcare.common.ApiResponse;
import com.example.petcare.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-items")
public class ServiceItemController {

    private final ServiceItemService serviceItemService;

    public ServiceItemController(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    @GetMapping
    public ApiResponse<PageResult<ServiceItem>> list(ServiceItemQuery query) {
        return ApiResponse.success(serviceItemService.list(query));
    }

    @GetMapping("/enabled")
    public ApiResponse<PageResult<ServiceItem>> listEnabled() {
        return ApiResponse.success(serviceItemService.listEnabled());
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceItem> detail(@PathVariable Long id) {
        return ApiResponse.success(serviceItemService.getByIdOrThrow(id));
    }

    @PostMapping
    public ApiResponse<ServiceItem> create(@Valid @RequestBody ServiceItemRequest request) {
        return ApiResponse.success(serviceItemService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ServiceItem> update(@PathVariable Long id, @Valid @RequestBody ServiceItemRequest request) {
        return ApiResponse.success(serviceItemService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<ServiceItem> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.success(serviceItemService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        serviceItemService.delete(id);
        return ApiResponse.success(null);
    }
}

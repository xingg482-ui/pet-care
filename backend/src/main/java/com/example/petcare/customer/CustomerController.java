package com.example.petcare.customer;

import com.example.petcare.common.ApiResponse;
import com.example.petcare.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ApiResponse<PageResult<Customer>> list(CustomerQuery query) {
        return ApiResponse.success(customerService.list(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<Customer> detail(@PathVariable Long id) {
        return ApiResponse.success(customerService.getByIdOrThrow(id));
    }

    @PostMapping
    public ApiResponse<Customer> create(@Valid @RequestBody CustomerRequest request) {
        return ApiResponse.success(customerService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Customer> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return ApiResponse.success(customerService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Customer> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.success(customerService.updateStatus(id, status));
    }
}

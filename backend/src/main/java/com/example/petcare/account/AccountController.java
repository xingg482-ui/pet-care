package com.example.petcare.account;

import com.example.petcare.common.ApiResponse;
import com.example.petcare.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ApiResponse<PageResult<AccountView>> list(
            AccountQuery query,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(accountService.list(query, authorization));
    }

    @GetMapping("/{id}")
    public ApiResponse<AccountView> detail(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(accountService.detail(id, authorization));
    }

    @PutMapping("/{id}/approve")
    public ApiResponse<AccountView> approve(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(accountService.approve(id, authorization));
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<AccountView> reject(
            @PathVariable Long id,
            @Valid @RequestBody AccountRejectRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(accountService.reject(id, request, authorization));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<AccountView> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AccountStatusRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(accountService.updateStatus(id, request, authorization));
    }
}

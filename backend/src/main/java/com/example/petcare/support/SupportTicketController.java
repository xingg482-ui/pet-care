package com.example.petcare.support;

import com.example.petcare.common.ApiResponse;
import com.example.petcare.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support/tickets")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    public SupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @PostMapping
    public ApiResponse<SupportTicketCreateResult> create(@Valid @RequestBody SupportTicketCreateRequest request) {
        return ApiResponse.success(supportTicketService.create(request));
    }

    @GetMapping("/public-query")
    public ApiResponse<SupportTicketView> publicQuery(@Valid SupportTicketPublicQuery query) {
        return ApiResponse.success(supportTicketService.publicQuery(query));
    }

    @GetMapping("/latest-public")
    public ApiResponse<SupportTicketView> latestPublic(@RequestParam String contactInfo) {
        return ApiResponse.success(supportTicketService.latestPublicConversation(contactInfo));
    }

    @PostMapping("/my")
    public ApiResponse<SupportTicketView> myCreate(
            @Valid @RequestBody SupportTicketMyCreateRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(supportTicketService.myCreate(request, authorization));
    }

    @GetMapping("/my")
    public ApiResponse<PageResult<SupportTicketView>> myList(
            SupportTicketQuery query,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(supportTicketService.myList(query, authorization));
    }

    @GetMapping("/my/{id}")
    public ApiResponse<SupportTicketView> myDetail(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(supportTicketService.myDetail(id, authorization));
    }

    @PostMapping("/my/{id}/replies")
    public ApiResponse<SupportTicketView> myReply(
            @PathVariable Long id,
            @Valid @RequestBody SupportTicketReplyRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(supportTicketService.myReply(id, request, authorization));
    }

    @GetMapping
    public ApiResponse<PageResult<SupportTicketView>> list(
            SupportTicketQuery query,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(supportTicketService.list(query, authorization));
    }

    @GetMapping("/{id}")
    public ApiResponse<SupportTicketView> detail(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(supportTicketService.detail(id, authorization));
    }

    @PostMapping("/{id}/replies")
    public ApiResponse<SupportTicketView> reply(
            @PathVariable Long id,
            @Valid @RequestBody SupportTicketReplyRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(supportTicketService.reply(id, request, authorization));
    }

    @PostMapping("/{id}/customer-messages")
    public ApiResponse<SupportTicketView> addCustomerMessage(
            @PathVariable Long id,
            @Valid @RequestBody SupportTicketCustomerMessageRequest request
    ) {
        return ApiResponse.success(supportTicketService.addCustomerMessage(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<SupportTicketView> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody SupportTicketStatusRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(supportTicketService.updateStatus(id, request, authorization));
    }
}

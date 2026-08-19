package com.example.petcare.ai;

import com.example.petcare.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(aiService.chat(request, authorization));
    }

    @GetMapping("/faqs")
    public ApiResponse<List<AiFaq>> faqs(@RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.success(aiService.listFaqs(authorization));
    }

    @GetMapping("/business-summary")
    public ApiResponse<AiBusinessSummary> businessSummary(@RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.success(aiService.businessSummary(authorization));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<AiChatSessionView>> sessions(@RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.success(aiService.listSessions(authorization));
    }

    @GetMapping("/sessions/{id}")
    public ApiResponse<AiChatSessionView> sessionDetail(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(aiService.sessionDetail(id, authorization));
    }
}

package com.example.petcare.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.petcare.account.AccountPrincipal;
import com.example.petcare.account.AccountService;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AiService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ADMIN = "ADMIN";
    private static final String CUSTOMER = "CUSTOMER";
    private static final String USER = "USER";
    private static final String ASSISTANT = "ASSISTANT";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AccountService accountService;
    private final AiFaqMapper aiFaqMapper;
    private final AiChatSessionMapper aiChatSessionMapper;
    private final AiChatMessageMapper aiChatMessageMapper;
    private final AiBusinessInsightService businessInsightService;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public AiService(
            AccountService accountService,
            AiFaqMapper aiFaqMapper,
            AiChatSessionMapper aiChatSessionMapper,
            AiChatMessageMapper aiChatMessageMapper,
            AiBusinessInsightService businessInsightService,
            @Value("${deepseek.api.base-url}") String baseUrl,
            @Value("${deepseek.api.key}") String apiKey,
            @Value("${deepseek.api.model}") String model,
            @Value("${deepseek.api.timeout-seconds}") long timeoutSeconds
    ) {
        this.accountService = accountService;
        this.aiFaqMapper = aiFaqMapper;
        this.aiChatSessionMapper = aiChatSessionMapper;
        this.aiChatMessageMapper = aiChatMessageMapper;
        this.businessInsightService = businessInsightService;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(clientHttpRequestFactory(timeoutSeconds))
                .build();
    }

    @Transactional
    public AiChatResponse chat(AiChatRequest request, String authorization) {
        AccountPrincipal principal = accountService.me(authorization);
        ensureRoleSupported(principal);
        AiChatSession session = resolveSession(request, principal);
        insertMessage(session.getId(), USER, request.message().trim(), null, null);

        Optional<AiFaq> matchedFaq = matchFaq(principal.role(), request.message());
        boolean businessQuestion = isStaff(principal.role()) && isBusinessQuestion(request.message(), matchedFaq.orElse(null));
        if (matchedFaq.isPresent() && !businessQuestion) {
            AiFaq faq = matchedFaq.get();
            String answer = faq.getAnswer();
            String riskLevel = riskLevel(answer);
            insertMessage(session.getId(), ASSISTANT, answer, "FAQ", riskLevel);
            touchSession(session.getId());
            return new AiChatResponse(session.getId(), answer, "FAQ", suggestedQuestions(principal.role()), riskLevel);
        }
        String businessContext = businessQuestion ? businessInsightService.promptContext() : "";
        if (businessQuestion && !StringUtils.hasText(apiKey) && matchedFaq.isPresent()) {
            String answer = matchedFaq.get().getAnswer() + "\n\n当前系统经营数据摘要：\n" + businessContext;
            String riskLevel = riskLevel(answer);
            insertMessage(session.getId(), ASSISTANT, answer, "FAQ_DATA", riskLevel);
            touchSession(session.getId());
            return new AiChatResponse(session.getId(), answer, "FAQ_DATA", suggestedQuestions(principal.role()), riskLevel);
        }

        DeepSeekChatResponse response = requestDeepSeek(principal, request, matchedFaq.map(AiFaq::getAnswer).orElse(""), businessContext);
        String answer = extractAnswer(response);
        String riskLevel = riskLevel(answer);
        insertMessage(session.getId(), ASSISTANT, answer, "DEEPSEEK", riskLevel);
        touchSession(session.getId());
        return new AiChatResponse(session.getId(), answer, "DEEPSEEK", suggestedQuestions(principal.role()), riskLevel);
    }

    public List<AiFaq> listFaqs(String authorization) {
        AccountPrincipal principal = accountService.me(authorization);
        ensureRoleSupported(principal);
        return aiFaqMapper.selectList(new LambdaQueryWrapper<AiFaq>()
                .eq(AiFaq::getEnabled, 1)
                .in(AiFaq::getRoleScope, allowedFaqScopes(principal.role()))
                .orderByAsc(AiFaq::getSortOrder)
                .orderByAsc(AiFaq::getId));
    }

    public AiBusinessSummary businessSummary(String authorization) {
        AccountPrincipal principal = accountService.requireStaff(authorization);
        ensureRoleSupported(principal);
        return businessInsightService.summary();
    }

    public List<AiChatSessionView> listSessions(String authorization) {
        AccountPrincipal principal = accountService.me(authorization);
        ensureRoleSupported(principal);
        return aiChatSessionMapper.selectList(new LambdaQueryWrapper<AiChatSession>()
                        .eq(AiChatSession::getAccountId, principal.accountId())
                        .orderByDesc(AiChatSession::getUpdatedAt)
                        .last("limit 20"))
                .stream()
                .map(session -> AiChatSessionView.from(session, List.of()))
                .toList();
    }

    public AiChatSessionView sessionDetail(Long id, String authorization) {
        AccountPrincipal principal = accountService.me(authorization);
        ensureRoleSupported(principal);
        AiChatSession session = getSessionForAccount(id, principal.accountId());
        List<AiChatMessageView> messages = aiChatMessageMapper.selectList(new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, id)
                        .orderByAsc(AiChatMessage::getCreatedAt)
                        .orderByAsc(AiChatMessage::getId))
                .stream()
                .map(AiChatMessageView::from)
                .toList();
        return AiChatSessionView.from(session, messages);
    }

    private DeepSeekChatResponse requestDeepSeek(AccountPrincipal principal, AiChatRequest request, String faqReference, String businessContext) {
        ensureApiKeyConfigured();
        DeepSeekChatRequest body = new DeepSeekChatRequest(
                model,
                List.of(
                        new DeepSeekMessage("system", systemPrompt(principal)),
                        new DeepSeekMessage("user", userPrompt(request, faqReference, businessContext))
                ),
                0.3,
                1200
        );
        try {
            return restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(DeepSeekChatResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401) {
                throw new IllegalArgumentException("DeepSeek API Key 无效，请检查环境变量 DEEPSEEK_API_KEY");
            }
            throw new IllegalArgumentException("AI 咨询服务暂时不可用，请稍后重试");
        } catch (RestClientException exception) {
            throw new IllegalArgumentException("AI 咨询服务连接失败，请稍后重试");
        }
    }

    private Optional<AiFaq> matchFaq(String role, String message) {
        String normalizedMessage = normalize(message);
        if (!StringUtils.hasText(normalizedMessage)) {
            return Optional.empty();
        }
        return listFaqsByRole(role).stream()
                .map(faq -> new FaqMatch(faq, faqScore(faq, normalizedMessage)))
                .filter(match -> match.score() >= 10)
                .max(Comparator.comparingInt(FaqMatch::score))
                .map(FaqMatch::faq);
    }

    private AiChatSession resolveSession(AiChatRequest request, AccountPrincipal principal) {
        if (request.sessionId() != null) {
            return getSessionForAccount(request.sessionId(), principal.accountId());
        }
        String now = now();
        AiChatSession session = new AiChatSession();
        session.setAccountId(principal.accountId());
        session.setRoleScope(principal.role());
        session.setTitle(buildSessionTitle(request.message()));
        session.setSourcePage(valueOrDash(request.sourcePage()));
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        aiChatSessionMapper.insert(session);
        return session;
    }

    private AiChatSession getSessionForAccount(Long id, Long accountId) {
        AiChatSession session = aiChatSessionMapper.selectById(id);
        if (session == null || !accountId.equals(session.getAccountId())) {
            throw new IllegalArgumentException("咨询会话不存在或无权限访问");
        }
        return session;
    }

    private void insertMessage(Long sessionId, String sender, String content, String source, String riskLevel) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setSender(sender);
        message.setContent(content);
        message.setSource(source);
        message.setRiskLevel(riskLevel);
        message.setCreatedAt(now());
        aiChatMessageMapper.insert(message);
    }

    private void touchSession(Long sessionId) {
        aiChatSessionMapper.update(new LambdaUpdateWrapper<AiChatSession>()
                .eq(AiChatSession::getId, sessionId)
                .set(AiChatSession::getUpdatedAt, now()));
    }

    private String buildSessionTitle(String message) {
        String value = message == null ? "AI 咨询" : message.trim().replaceAll("\\s+", " ");
        if (!StringUtils.hasText(value)) {
            return "AI 咨询";
        }
        return value.length() > 24 ? value.substring(0, 24) + "..." : value;
    }

    private List<AiFaq> listFaqsByRole(String role) {
        return aiFaqMapper.selectList(new LambdaQueryWrapper<AiFaq>()
                .eq(AiFaq::getEnabled, 1)
                .in(AiFaq::getRoleScope, allowedFaqScopes(role))
                .orderByAsc(AiFaq::getSortOrder)
                .orderByAsc(AiFaq::getId));
    }

    private List<String> allowedFaqScopes(String role) {
        if (CUSTOMER.equals(role)) {
            return List.of("ALL", CUSTOMER);
        }
        return List.of("ALL", ADMIN);
    }

    private int faqScore(AiFaq faq, String normalizedMessage) {
        int score = 0;
        String question = normalize(faq.getQuestion());
        if (normalizedMessage.equals(question)) {
            score += 100;
        }
        if (normalizedMessage.contains(question) || question.contains(normalizedMessage)) {
            score += 60;
        }
        String category = normalize(faq.getCategory());
        if (StringUtils.hasText(category) && normalizedMessage.contains(category)) {
            score += 12;
        }
        for (String keyword : String.valueOf(faq.getKeywords()).split("[,，]")) {
            String normalizedKeyword = normalize(keyword);
            if (StringUtils.hasText(normalizedKeyword) && normalizedMessage.contains(normalizedKeyword)) {
                score += Math.max(10, Math.min(28, normalizedKeyword.length() * 4));
            }
        }
        return score;
    }

    private String systemPrompt(AccountPrincipal principal) {
        boolean customer = CUSTOMER.equals(principal.role());
        String roleRule = customer
                ? "当前用户是客户。只能回答客户自己的宠物、订单、托管、健康记录、门店服务流程和日常养宠建议。禁止透露门店成本、利润、财务分析、客户排行、其他客户资料。"
                : "当前用户是管理员。可以回答门店服务 SOP、客户维护思路、服务项目优化、财务经营分析和利润提升建议。当前阶段没有注入实时经营数据，如需要具体数字，必须提示用户以系统财务页和订单页为准。";
        return """
                你是宠物管理系统内置的 AI 咨询助手，请用简体中文回答。
                回答要温和、专业、清晰，优先给出可执行步骤。
                %s
                涉及宠物疾病诊断、药物、剂量、紧急健康风险时，必须提醒用户咨询兽医或立即就医，不能给确定诊断。
                涉及财务、利润、经营建议时，说明这是基于系统数据和常识的辅助建议，不是审计结论。
                回答控制在 600 字以内，必要时用短列表。
                """.formatted(roleRule);
    }

    private String userPrompt(AiChatRequest request, String faqReference, String businessContext) {
        return """
                来源页面：%s
                上下文类型：%s
                上下文 ID：%s
                FAQ 参考回答：%s
                经营数据上下文：%s
                用户问题：%s
                """.formatted(
                valueOrDash(request.sourcePage()),
                valueOrDash(request.contextType()),
                request.contextId() == null ? "-" : request.contextId(),
                valueOrDash(faqReference),
                valueOrDash(businessContext),
                request.message().trim()
        );
    }

    private String extractAnswer(DeepSeekChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalArgumentException("AI 暂时没有返回有效回答，请稍后重试");
        }
        DeepSeekMessage message = response.choices().getFirst().message();
        if (message == null || !StringUtils.hasText(message.content())) {
            throw new IllegalArgumentException("AI 暂时没有返回有效回答，请稍后重试");
        }
        return message.content().trim();
    }

    private void ensureRoleSupported(AccountPrincipal principal) {
        if (!SUPER_ADMIN.equals(principal.role()) && !ADMIN.equals(principal.role()) && !CUSTOMER.equals(principal.role())) {
            throw new IllegalArgumentException("当前账号无权使用 AI 咨询");
        }
    }

    private boolean isStaff(String role) {
        return SUPER_ADMIN.equals(role) || ADMIN.equals(role);
    }

    private boolean isBusinessQuestion(String message, AiFaq matchedFaq) {
        String text = normalize((message == null ? "" : message)
                + " "
                + (matchedFaq == null ? "" : matchedFaq.getCategory())
                + " "
                + (matchedFaq == null ? "" : matchedFaq.getQuestion()));
        return List.of("优质客户", "客户价值", "优质项目", "服务项目", "财务", "营收", "成本", "利润", "利润率", "赚钱", "套餐", "复购", "经营分析")
                .stream()
                .map(this::normalize)
                .anyMatch(text::contains);
    }

    private void ensureApiKeyConfigured() {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("DeepSeek API Key 未配置，请先设置环境变量 DEEPSEEK_API_KEY 并重启后端");
        }
    }

    private List<String> suggestedQuestions(String role) {
        if (CUSTOMER.equals(role)) {
            return List.of("洗澡美容服务包含哪些流程？", "我的订单状态是什么意思？", "托管期间每天会做哪些照护？", "疫苗后多久可以洗澡？");
        }
        return List.of("帮我分析哪些服务项目更值得推广", "如何识别优质客户？", "本月财务状况应该怎么看？", "如何提高赚取利润的效率？");
    }

    private String riskLevel(String answer) {
        String content = answer == null ? "" : answer;
        return content.contains("兽医") || content.contains("就医") || content.contains("诊断") ? "MEDICAL_NOTICE" : "LOW";
    }

    private String valueOrDash(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace("？", "?")
                .replace("“", "")
                .replace("”", "")
                .replace("‘", "")
                .replace("’", "")
                .replace(" ", "")
                .trim();
    }

    private String now() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    private org.springframework.http.client.ClientHttpRequestFactory clientHttpRequestFactory(long timeoutSeconds) {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        int timeoutMillis = (int) Duration.ofSeconds(timeoutSeconds).toMillis();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return factory;
    }

    private record DeepSeekChatRequest(
            String model,
            List<DeepSeekMessage> messages,
            Double temperature,
            @JsonProperty("max_tokens") Integer maxTokens
    ) {
    }

    private record DeepSeekMessage(String role, String content) {
    }

    private record DeepSeekChatResponse(List<DeepSeekChoice> choices) {
    }

    private record DeepSeekChoice(DeepSeekMessage message) {
    }

    private record FaqMatch(AiFaq faq, int score) {
    }
}

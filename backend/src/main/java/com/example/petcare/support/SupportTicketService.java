package com.example.petcare.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.petcare.account.AccountPrincipal;
import com.example.petcare.account.AccountService;
import com.example.petcare.common.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class SupportTicketService {

    private static final String OPEN = "OPEN";
    private static final String REPLIED = "REPLIED";
    private static final String CLOSED = "CLOSED";
    private static final String CUSTOMER = "CUSTOMER";
    private static final String OTHER = "OTHER";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SupportTicketMapper ticketMapper;
    private final SupportTicketReplyMapper replyMapper;
    private final AccountService accountService;

    public SupportTicketService(SupportTicketMapper ticketMapper, SupportTicketReplyMapper replyMapper, AccountService accountService) {
        this.ticketMapper = ticketMapper;
        this.replyMapper = replyMapper;
        this.accountService = accountService;
    }

    @Transactional
    public SupportTicketCreateResult create(SupportTicketCreateRequest request) {
        String now = now();
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNo(nextTicketNo());
        ticket.setContactName(trimRequired(request.contactName(), "联系人名称"));
        ticket.setContactInfo(trimRequired(request.contactInfo(), "联系方式"));
        ticket.setUsername(trimToNull(request.username()));
        ticket.setIssueType(normalizeIssueType(request.issueType()));
        ticket.setContent(limitContent(request.content()));
        ticket.setStatus(OPEN);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        ticketMapper.insert(ticket);
        return new SupportTicketCreateResult(ticket.getTicketNo(), "咨询已提交，请保存咨询编号");
    }

    public SupportTicketView publicQuery(SupportTicketPublicQuery query) {
        SupportTicket ticket = ticketMapper.selectOne(new LambdaQueryWrapper<SupportTicket>()
                .eq(SupportTicket::getTicketNo, query.ticketNo().trim())
                .eq(SupportTicket::getContactInfo, query.contactInfo().trim()));
        if (ticket == null) {
            throw new IllegalArgumentException("未找到匹配的咨询记录");
        }
        return toView(ticket);
    }

    public SupportTicketView latestPublicConversation(String contactInfo) {
        SupportTicket ticket = ticketMapper.selectOne(new LambdaQueryWrapper<SupportTicket>()
                .eq(SupportTicket::getContactInfo, trimRequired(contactInfo, "联系方式"))
                .orderByDesc(SupportTicket::getUpdatedAt)
                .last("limit 1"));
        if (ticket == null) {
            throw new IllegalArgumentException("暂无聊天记录，请先发送消息");
        }
        return toView(ticket);
    }

    public PageResult<SupportTicketView> myList(SupportTicketQuery query, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        LambdaQueryWrapper<SupportTicket> wrapper = myTicketWrapper(principal)
                .eq(StringUtils.hasText(query.getStatus()), SupportTicket::getStatus, query.getStatus())
                .orderByDesc(SupportTicket::getUpdatedAt);
        Page<SupportTicket> page = ticketMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        List<SupportTicketView> records = page.getRecords().stream().map(ticket -> SupportTicketView.from(ticket, List.of())).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public SupportTicketView myCreate(SupportTicketMyCreateRequest request, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        String now = now();
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNo(nextTicketNo());
        ticket.setContactName(trimRequired(principal.displayName(), "联系人名称"));
        ticket.setContactInfo(trimRequired(principal.username(), "联系方式"));
        ticket.setUsername(principal.username());
        ticket.setIssueType(OTHER);
        ticket.setContent(limitContent(request.content()));
        ticket.setStatus(OPEN);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        ticketMapper.insert(ticket);
        return toView(ticket);
    }

    public SupportTicketView myDetail(Long id, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        SupportTicket ticket = getByIdOrThrow(id);
        ensureOwner(ticket, principal);
        return toView(ticket);
    }

    @Transactional
    public SupportTicketView myReply(Long id, SupportTicketReplyRequest request, String authorization) {
        AccountPrincipal principal = accountService.requireCustomer(authorization);
        SupportTicket ticket = getByIdOrThrow(id);
        ensureOwner(ticket, principal);
        if (CLOSED.equals(ticket.getStatus())) {
            throw new IllegalArgumentException("会话已关闭，请重新发起咨询");
        }
        String now = now();
        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicketId(id);
        reply.setReplierAccountId(principal.accountId());
        reply.setReplierRole(CUSTOMER);
        reply.setContent(limitContent(request.content()));
        reply.setCreatedAt(now);
        replyMapper.insert(reply);
        ticketMapper.update(new LambdaUpdateWrapper<SupportTicket>()
                .eq(SupportTicket::getId, id)
                .set(SupportTicket::getStatus, OPEN)
                .set(SupportTicket::getUpdatedAt, now));
        return toView(getByIdOrThrow(id));
    }

    public PageResult<SupportTicketView> list(SupportTicketQuery query, String authorization) {
        accountService.requireStaff(authorization);
        LambdaQueryWrapper<SupportTicket> wrapper = new LambdaQueryWrapper<SupportTicket>()
                .like(StringUtils.hasText(query.getTicketNo()), SupportTicket::getTicketNo, query.getTicketNo())
                .like(StringUtils.hasText(query.getContactName()), SupportTicket::getContactName, query.getContactName())
                .like(StringUtils.hasText(query.getContactInfo()), SupportTicket::getContactInfo, query.getContactInfo())
                .like(StringUtils.hasText(query.getUsername()), SupportTicket::getUsername, query.getUsername())
                .eq(StringUtils.hasText(query.getIssueType()), SupportTicket::getIssueType, query.getIssueType())
                .eq(StringUtils.hasText(query.getStatus()), SupportTicket::getStatus, query.getStatus())
                .orderByDesc(SupportTicket::getUpdatedAt);
        Page<SupportTicket> page = ticketMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        List<SupportTicketView> records = page.getRecords().stream().map(ticket -> SupportTicketView.from(ticket, List.of())).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public SupportTicketView detail(Long id, String authorization) {
        accountService.requireStaff(authorization);
        return toView(getByIdOrThrow(id));
    }

    @Transactional
    public SupportTicketView reply(Long id, SupportTicketReplyRequest request, String authorization) {
        AccountPrincipal principal = accountService.requireStaff(authorization);
        SupportTicket ticket = getByIdOrThrow(id);
        if (CLOSED.equals(ticket.getStatus())) {
            throw new IllegalArgumentException("已关闭咨询不能回复，请先重新打开");
        }
        String now = now();
        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicketId(id);
        reply.setReplierAccountId(principal.accountId());
        reply.setReplierRole(principal.role());
        reply.setContent(limitContent(request.content()));
        reply.setCreatedAt(now);
        replyMapper.insert(reply);
        ticketMapper.update(new LambdaUpdateWrapper<SupportTicket>()
                .eq(SupportTicket::getId, id)
                .set(SupportTicket::getStatus, REPLIED)
                .set(SupportTicket::getUpdatedAt, now));
        return toView(getByIdOrThrow(id));
    }

    @Transactional
    public SupportTicketView addCustomerMessage(Long id, SupportTicketCustomerMessageRequest request) {
        SupportTicket ticket = getByIdOrThrow(id);
        if (CLOSED.equals(ticket.getStatus())) {
            throw new IllegalArgumentException("会话已关闭，请重新发起咨询");
        }
        String contactInfo = trimRequired(request.contactInfo(), "联系方式");
        if (!ticket.getContactInfo().equals(contactInfo)) {
            throw new IllegalArgumentException("联系方式与会话不匹配");
        }
        String now = now();
        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicketId(id);
        reply.setReplierAccountId(null);
        reply.setReplierRole(CUSTOMER);
        reply.setContent(limitContent(request.content()));
        reply.setCreatedAt(now);
        replyMapper.insert(reply);
        ticketMapper.update(new LambdaUpdateWrapper<SupportTicket>()
                .eq(SupportTicket::getId, id)
                .set(SupportTicket::getStatus, OPEN)
                .set(SupportTicket::getUpdatedAt, now));
        return toView(getByIdOrThrow(id));
    }

    @Transactional
    public SupportTicketView updateStatus(Long id, SupportTicketStatusRequest request, String authorization) {
        accountService.requireStaff(authorization);
        String status = normalizeStatus(request.status());
        ticketMapper.update(new LambdaUpdateWrapper<SupportTicket>()
                .eq(SupportTicket::getId, id)
                .set(SupportTicket::getStatus, status)
                .set(SupportTicket::getUpdatedAt, now()));
        return toView(getByIdOrThrow(id));
    }

    private SupportTicketView toView(SupportTicket ticket) {
        List<SupportTicketReplyView> replies = replyMapper.selectList(new LambdaQueryWrapper<SupportTicketReply>()
                        .eq(SupportTicketReply::getTicketId, ticket.getId())
                        .orderByAsc(SupportTicketReply::getCreatedAt))
                .stream()
                .map(SupportTicketReplyView::from)
                .toList();
        return SupportTicketView.from(ticket, replies);
    }

    private SupportTicket getByIdOrThrow(Long id) {
        SupportTicket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new IllegalArgumentException("咨询不存在");
        }
        return ticket;
    }

    private LambdaQueryWrapper<SupportTicket> myTicketWrapper(AccountPrincipal principal) {
        return new LambdaQueryWrapper<SupportTicket>()
                .and(wrapper -> wrapper
                        .eq(SupportTicket::getUsername, principal.username())
                        .or()
                        .eq(SupportTicket::getContactInfo, principal.username()));
    }

    private void ensureOwner(SupportTicket ticket, AccountPrincipal principal) {
        if (!principal.username().equals(ticket.getUsername()) && !principal.username().equals(ticket.getContactInfo())) {
            throw new IllegalArgumentException("无权限访问该客服会话");
        }
    }

    private String nextTicketNo() {
        return "ST" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
    }

    private String normalizeIssueType(String issueType) {
        String value = issueType.trim().toUpperCase(Locale.ROOT);
        if (!List.of("LOGIN_FAILED", "REVIEW_QUERY", "ACCOUNT_DISABLED", "PASSWORD", "OTHER").contains(value)) {
            throw new IllegalArgumentException("问题类型不合法");
        }
        return value;
    }

    private String normalizeStatus(String status) {
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (!List.of(OPEN, REPLIED, CLOSED).contains(value)) {
            throw new IllegalArgumentException("咨询状态不合法");
        }
        return value;
    }

    private String limitContent(String content) {
        String value = trimRequired(content, "内容");
        if (value.length() > 500) {
            throw new IllegalArgumentException("内容不能超过 500 字");
        }
        return value;
    }

    private String trimRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String now() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}

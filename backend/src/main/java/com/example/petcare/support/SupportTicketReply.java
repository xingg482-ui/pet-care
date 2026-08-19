package com.example.petcare.support;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("support_ticket_reply")
public class SupportTicketReply {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private Long replierAccountId;
    private String replierRole;
    private String content;
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Long getReplierAccountId() { return replierAccountId; }
    public void setReplierAccountId(Long replierAccountId) { this.replierAccountId = replierAccountId; }
    public String getReplierRole() { return replierRole; }
    public void setReplierRole(String replierRole) { this.replierRole = replierRole; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

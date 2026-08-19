package com.example.petcare.support;

public class SupportTicketQuery {

    private String ticketNo;
    private String contactName;
    private String contactInfo;
    private String username;
    private String issueType;
    private String status;
    private long page = 1;
    private long pageSize = 10;

    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getPage() { return page; }
    public void setPage(long page) { this.page = Math.max(page, 1); }
    public long getPageSize() { return pageSize; }
    public void setPageSize(long pageSize) { this.pageSize = pageSize <= 0 ? 10 : Math.min(pageSize, 100); }
}

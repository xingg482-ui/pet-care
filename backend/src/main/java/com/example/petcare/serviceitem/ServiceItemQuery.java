package com.example.petcare.serviceitem;

public class ServiceItemQuery {

    private String name;
    private String category;
    private String status;
    private long page = 1;
    private long pageSize = 10;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = Math.max(page, 1);
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize <= 0 ? 10 : Math.min(pageSize, 100);
    }
}

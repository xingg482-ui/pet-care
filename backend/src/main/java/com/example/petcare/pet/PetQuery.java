package com.example.petcare.pet;

public class PetQuery {

    private String name;
    private Long customerId;
    private String species;
    private String status;
    private long page = 1;
    private long pageSize = 10;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
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

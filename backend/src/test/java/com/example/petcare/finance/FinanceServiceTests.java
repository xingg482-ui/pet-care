package com.example.petcare.finance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FinanceServiceTests {

    @Autowired
    private FinanceService financeService;

    @Test
    void summaryAndServiceItemStatsCanLoad() {
        assertThat(financeService.summary()).isNotNull();
        assertThat(financeService.serviceItems(new FinanceQuery())).isNotNull();
    }
}

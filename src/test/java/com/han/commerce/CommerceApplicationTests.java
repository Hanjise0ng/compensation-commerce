package com.han.commerce;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CommerceApplicationTests {

    @Test
    @DisplayName("애플리케이션 컨텍스트가 실제 MySQL 위에서 뜬다")
    void contextLoads() {
    }

}
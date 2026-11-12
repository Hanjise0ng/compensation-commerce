package com.han.commerce.support;

import com.han.commerce.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

    @Autowired
    protected DatabaseCleaner databaseCleaner;

    @Autowired
    protected Fixtures fixtures;

    @Autowired
    protected MutableClockHolder clock;

    @BeforeEach
    void resetState() {
        databaseCleaner.clean();
        clock.reset();
    }

}
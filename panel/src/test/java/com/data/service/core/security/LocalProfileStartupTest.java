package com.data.service.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class LocalProfileStartupTest {

    @Autowired
    private Environment environment;

    @Test
    void localProfileDisablesSslForLocalDevelopment() {
        assertThat(environment.getProperty("server.ssl.enabled", Boolean.class)).isFalse();
    }
}

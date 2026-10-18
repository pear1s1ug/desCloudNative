package com.backend;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(BackendApplicationTests.RabbitAdminTestConfiguration.class)
class BackendApplicationTests {

    @TestConfiguration
    static class RabbitAdminTestConfiguration {
        @Bean
        RabbitAdmin rabbitAdmin() {
            return Mockito.mock(RabbitAdmin.class);
        }
    }

    @Test
	void contextLoads() {
	}

}

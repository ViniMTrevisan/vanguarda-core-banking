package com.vinicius.vanguarda.integration;

import com.vinicius.vanguarda.TestContainersConfig;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfig.class, AbstractIntegrationTest.RedissonTestConfig.class})
@Testcontainers
abstract class AbstractIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "testpass");

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "testpass");
    }

    @TestConfiguration
    static class RedissonTestConfig {
        @Bean
        RedissonClient redissonClient() {
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://" + redis.getHost() + ":" + redis.getMappedPort(6379))
                    .setPassword("testpass")
                    .setTimeout(2000)
                    .setConnectTimeout(2000);
            return Redisson.create(config);
        }
    }
}

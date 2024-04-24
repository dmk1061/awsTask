package test.aws.task.service;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import reactor.test.StepVerifier;
import test.aws.task.config.ConfigRedis;

import java.time.Duration;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(value = {ConfigRedis.class})
public class RedisServiceTest extends ContainerRedisClusterTest {

    @Value("${redisson.cluster-servers-config.node-addresses}")
    private List<String> nodeAddresses;
    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:latest")
            .withExposedPorts(6379);

    private RedisService redisService = new RedisService(reactiveRedisTemplate, ContainerRedisClusterTest.redissonClient, "100MB");;

    @Test
    public void testSetData() {
        StepVerifier.create(redisService.setData("user1", "id1", "data1"))
                .expectNext(true)
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    public void testGetData() {
        StepVerifier.create(redisService.getData("user1", "id1"))
                .expectNext("data1")
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }


}
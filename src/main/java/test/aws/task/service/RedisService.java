package test.aws.task.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class RedisService implements DataService {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ReactiveHashOperations<String, String, String> reactiveHashOperations;
    private final long memoryLimit;
    public static final String USER_PREFIX = "u:";
    public static final String MESSAGE_PREFIX = "m:";
    public static final String LOCK_PREFIX = "l:";
    private final RedissonClient redissonClient;

    @Autowired
    public RedisService(ReactiveRedisTemplate<String, String> reactiveRedisTemplate, RedissonClient redissonClient,
                        @Value("${user.memory-limit}") String memoryLimit) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.reactiveHashOperations = reactiveRedisTemplate.opsForHash();
        this.memoryLimit = parseMemoryLimit(memoryLimit);
        this.redissonClient = redissonClient;
    }

    @Override
    public Mono<Boolean> setData(String userId, String id, String data) {
        String hashKey = USER_PREFIX + userId;
        String listKey = MESSAGE_PREFIX + userId;
        RLock lock = redissonClient.getLock(LOCK_PREFIX + userId);
        Mono<Boolean> result;
        try {
            result = reactiveRedisTemplate.opsForHash().put(hashKey, id, data)
                    .flatMap(success -> {
                        if (success) {
                            return reactiveRedisTemplate.opsForList().leftPush(listKey, id)
                                    .flatMap(currentSize -> {
                                        log.info(currentSize + ">" + memoryLimit);
                                        while (currentSize > memoryLimit) {
                                            log.info("currentSize > memoryLimit");
                                            return reactiveRedisTemplate.opsForList().rightPop(listKey)
                                                    .flatMap(removedId -> {
                                                        if (removedId != null) {
                                                            log.info("removeId : " + removedId);
                                                            log.info(hashKey);
                                                            return reactiveRedisTemplate.opsForHash().remove(hashKey, removedId)
                                                                    .then(reactiveRedisTemplate.opsForList().size(listKey))
                                                                    .map(size -> currentSize - 1);
                                                        } else {
                                                            log.info("removeId=null");
                                                            return Mono.just(currentSize);
                                                        }
                                                    });
                                        }
                                        return Mono.just(true);
                                    })
                                    .map(success2 -> true);
                        } else {
                            return Mono.just(false);
                        }
                    });
        } finally {
            if (lock != null && lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }

        }
        return result;

    }

    @Override
    public Mono<String> getData(String userId, String id) {
        String hashKey = USER_PREFIX + userId;
        return reactiveHashOperations.get(hashKey, id);
    }

    private long parseMemoryLimit(String memoryLimit) {
        long multiplier = 1;
        if (memoryLimit.endsWith("KB")) {
            multiplier = 1024;
        } else if (memoryLimit.endsWith("MB")) {
            multiplier = 1024 * 1024;
        } else if (memoryLimit.endsWith("GB")) {
            multiplier = 1024 * 1024 * 1024;
        }
        return Long.parseLong(memoryLimit.replaceAll("[^\\d.]", "")) * multiplier;
    }
}
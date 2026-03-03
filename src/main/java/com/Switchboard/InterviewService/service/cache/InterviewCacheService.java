package com.Switchboard.InterviewService.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewCacheService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "interview:";
    private static final String INDEX_KEY = "interview:index:lastUpdated";
    private static final Duration TTL = Duration.ofHours(6);

    public void save(UUID id, String data) {

        redisTemplate.opsForValue()
                .set(PREFIX + id, data, TTL);
    }

    public String get(UUID id) {

        String key = PREFIX + id;

        String value = redisTemplate.opsForValue().get(key);

        if (value != null) {
            log.info("Redis GET SUCCESS :: {}", key);
        } else {
            log.info("Redis GET MISS :: {}", key);
        }

        return value;
    }


    public void delete(UUID id) {

        redisTemplate.delete(PREFIX + id);
    }

    public void addToIndex(UUID id, long score) {

        redisTemplate.opsForZSet()
                .add(INDEX_KEY, id.toString(), (double) score);
    }

    public Set<String> getLatestIds(int start, int end) {

        log.info("Redis ZSET query :: {} start={} end={}", INDEX_KEY, start, end);

        return redisTemplate.opsForZSet()
                .reverseRange(INDEX_KEY, start, end);
    }

    public void removeFromIndex(UUID id) {

        redisTemplate.opsForZSet()
                .remove(INDEX_KEY, id.toString());
    }
    public void trimIndex(int maxSize) {

        redisTemplate.opsForZSet()
                .removeRange(INDEX_KEY, 0, -(maxSize + 1));
    }
}
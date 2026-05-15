package mju.capstone.ddingconnect.global.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 🔹 Redis 편의 메서드를 모아 둔 유틸리티 클래스
 *    - 문자열(String) 타입 전용 StringRedisTemplate을 주입받아 사용
 *    - 기본 CRUD + 만료시간 설정 기능 제공
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class RedisUtil {
    private final StringRedisTemplate stringRedisTemplate;

    /**
     *  key로부터 value 조회
     *  @return 값이 없으면 null
     *  */
    public String getData(String key) {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        return valueOperations.get(key);
    }

    /**
     *  해당 key 존재 여부 확인
     *  @return 존재하면 true, 없으면 false
     *  */
    public boolean existData(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }


    /**
     *  만료시간 없이 key‑value 저장
     *  */
    public void setData(String key, String value) {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        valueOperations.set(key, value);
    }


    /**
     *  만료시간(duration 초)과 함께 key‑value 저장
     *  @param duration 초 단위 TTL(Time To Live)
     *  */
    public void setDataWithExpire(String key, String value, long duration) {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        Duration expireDuration = Duration.ofSeconds(duration);
        valueOperations.set(key, value, expireDuration);
    }

    /**
     *  key 삭제
     *  */
    public void deleteData(String key) {
        stringRedisTemplate.delete(key);
    }





}

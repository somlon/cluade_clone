package mju.capstone.ddingconnect.domain.coffeechat.domain.repository;

import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * [커피챗 알람 레포지토리]
 * CoffeeChatAlarm 엔티티에 대한 데이터베이스 접근 인터페이스
 *
 * 알람 행 1건 = 수신자 1명 구조 (요청자/수신자 각각 별도 행으로 INSERT)
 */
public interface CoffeeChatAlarmRepository extends JpaRepository<CoffeeChatAlarm, Long> {

    List<CoffeeChatAlarm> findByCoffeeChatId(Long coffeeChatId);

    List<CoffeeChatAlarm> findByCoffeeChatIdAndIsRead(Long coffeeChatId, Boolean isRead);

    /** 알람 수신자(member) 기준 조회 */
    List<CoffeeChatAlarm> findByMemberId(Long memberId);
}

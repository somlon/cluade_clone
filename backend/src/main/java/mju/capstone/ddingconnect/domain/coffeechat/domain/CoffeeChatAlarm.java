package mju.capstone.ddingconnect.domain.coffeechat.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.common.BaseEntity;

/**
 * [커피챗 알람 엔티티]
 *
 * ERD 컬럼 매핑:
 * - PK(Long)          → id
 * - FK(Long)          → coffeeChat (커피챗.PK 참조)
 * - FK2(Long)         → member (회원.PK 참조, 알람 수신자) ※ ERD 보완 사항
 * - 알람 내용          → content (varchar(255))
 * - 읽음 여부(Boolean) → isRead
 *
 * 연결 관계:
 * - 커피챗(CoffeeChat): N:1 (커피챗알람.FK → 커피챗.PK)
 * - 회원(Member): N:1 (커피챗알람.FK2 → 회원.PK, 수신자 추적)
 *
 * 발행 정책:
 * - 한 이벤트당 요청자용/수신자용 2건의 알람 행을 별도로 INSERT
 * - 각 알람의 isRead는 해당 수신자에게만 영향
 */
@Entity
@Table(name = "coffee_chat_alarm")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoffeeChatAlarm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coffee_chat_id", nullable = false)
    private CoffeeChat coffeeChat;  // ERD의 FK → 커피챗.PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;          // 알람 수신자 (요청자/수신자 중 1명)

    @Column(length = 255)
    private String content;         // 알람 내용

    private Boolean isRead;         // 읽음 여부
}

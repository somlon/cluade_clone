package mju.capstone.ddingconnect.domain.coffeechat.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.common.BaseEntity;

import java.time.LocalDateTime;

/**
 * [커피챗 엔티티]
 * ERD 컬럼 매핑:
 * - PK(Long)              → id
 * - 요청자(Long)           → requester (회원.PK 참조)
 * - 수신자(Long)           → receiver (회원.PK 참조)
 * - 직무 점수(Integer)     → jobScore
 * - 역량 점수(Integer)     → ability
 * - 목표기업 점수(Integer) → goal
 * - 수략 여부(ENUM)        → status (CoffeeChatStatus)
 * - 수정 시간(datetime)    → updateAt
 * - 카카오 오픈체팅 링크    → kakaoOpenChatLink (varchar(255))
 *
 * 연결 관계:
 * - 요청자 회원(Member): N:1 (커피챗.요청자 → 회원.PK)
 * - 수신자 회원(Member): N:1 (커피챗.수신자 → 회원.PK)
 * - 커피챗알람(CoffeeChatAlarm): 1:N (CoffeeChatAlarm.FK → 커피챗.PK)
 */
@Entity
@Table(name = "coffee_chat")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoffeeChat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private Member requester;           // ERD의 요청자 → 회원.PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Member receiver;            // ERD의 수신자 → 회원.PK

    private Integer jobScore;           // 직무 점수

    private Integer ability;             // 역량 점수

    private Integer goal;               // 목표기업 점수

    @Enumerated(EnumType.STRING)
    private CoffeeChatStatus status;    // 수략 여부(ENUM)

    private LocalDateTime updateAt;      // 수정 시간(datetime)

    @Column(length = 255)
    private String kakaoOpenChatLink;   // 카카오 오픈체팅 링크
}

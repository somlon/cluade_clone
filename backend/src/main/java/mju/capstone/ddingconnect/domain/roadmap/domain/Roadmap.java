package mju.capstone.ddingconnect.domain.roadmap.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.common.BaseEntity;

/**
 * [로드맵 엔티티]
 * ERD 컬럼 매핑:
 * - PK(Long)    → id
 * - FK(Long)    → member (회원.PK 참조)
 * - 내용(TEXT)  → content (columnDefinition = "TEXT")
 *
 * 연결 관계:
 * - 회원(Member): N:1 (로드맵.FK → 회원.PK)
 * - 로드맵알람(RoadmapAlarm): 1:N (RoadmapAlarm.PK2 → 로드맵.PK)
 */
@Entity
@Table(name = "roadmap")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Roadmap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;   // ERD의 FK → 회원.PK

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;  // 로드맵 본문 (일반 문자열, 형식 미강제)
}

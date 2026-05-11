package mju.capstone.ddingconnect.domain.roadmap.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.common.BaseEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * [로드맵 엔티티]
 * ERD 컬럼 매핑:
 * - PK(Long)    → id
 * - FK(Long)    → member (회원.PK 참조)
 * - 내용(JSON)  → content (columnDefinition = "JSON")
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content")
    private String content;  // ERD의 내용(JSON) - Hibernate 6 JSON 타입 매핑
}

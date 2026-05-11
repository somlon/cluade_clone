package mju.capstone.ddingconnect.domain.roadmap.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.global.common.BaseEntity;

/**
 * [로드맵 알람 엔티티]
 * ERD 컬럼 매핑 (ERD에 명시된 컬럼 그대로):
 * - PK(Long)           → id
 * - PK2(Long)          → roadmap (로드맵.PK 참조)
 * - 알람 내용           → content (varchar(255))
 * - 읽음 여부(Boolean)  → isRead
 *
 * 연결 관계:
 * - 로드맵(Roadmap): N:1 (로드맵알람.PK2 → 로드맵.PK)
 */
@Entity
@Table(name = "roadmap_alarm")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapAlarm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmap_id", nullable = false)
    private Roadmap roadmap;  // ERD의 PK2 → 로드맵.PK

    @Column(length = 255)
    private String content;   // 알람 내용

    private Boolean isRead;   // 읽음 여부
}

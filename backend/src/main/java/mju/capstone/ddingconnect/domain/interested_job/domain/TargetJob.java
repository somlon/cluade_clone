package mju.capstone.ddingconnect.domain.interested_job.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.common.BaseEntity;

/**
 * [관심 직군 엔티티]
 * 회원의 직군 카테고리 선호도(마이페이지 칩). 구직 공고(PostContents)와는 무관.
 *
 * 컬럼:
 * - PK(Long)              → id
 * - FK(Long)              → member (회원.PK 참조)
 * - Interested_Job(ENUM)  → interestedJob (TargetJobCategory, column: Interested_Job)
 * - Key2(varchar(255))    → key2 (현재 미사용, ERD 잔재)
 *
 * 연결 관계:
 * - 회원(Member): N:1 (관심직군.FK → 회원.PK)
 *
 * 공고와의 매칭(예: "내 관심 카테고리에 해당하는 새 공고")은 FK가 아닌
 * TargetJobCategory ↔ JobType enum 값 매칭으로 처리한다.
 */
@Entity
@Table(name = "target_job")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;              // ERD의 FK → 회원.PK

    @Enumerated(EnumType.STRING)
    @Column(name = "Interested_Job")
    private TargetJobCategory interestedJob;  // 관심 직군(ENUM)

    @Column(length = 255)
    private String key2;                // Key2 (미사용)
}

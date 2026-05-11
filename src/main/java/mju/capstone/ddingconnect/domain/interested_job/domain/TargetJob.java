package mju.capstone.ddingconnect.domain.interested_job.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.domain.job_post.domain.PostContents;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.common.BaseEntity;

/**
 * [관심 직군 엔티티]
 * ERD 컬럼 매핑:
 * - PK(Long)              → id
 * - FK(Long)              → member (회원.PK 참조)
 * - FK2(Long)             → postContents (구직공고.PK 참조)
 * - Interested_Job(ENUM)  → interestedJob (TargetJobCategory, column: Interested_Job)
 * - Key2(varchar(255))    → key2
 *
 * 연결 관계:
 * - 회원(Member): N:1 (관심직군.FK → 회원.PK)
 * - 구직공고(PostContents): N:1 (관심직군.FK2 → 구직공고.PK)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_post_id", nullable = false)
    private PostContents postContents;  // ERD의 FK2 → 구직공고.PK

    @Enumerated(EnumType.STRING)
    @Column(name = "Interested_Job")
    private TargetJobCategory interestedJob;  // 관심 직군(ENUM)

    @Column(length = 255)
    private String key2;                // Key2
}

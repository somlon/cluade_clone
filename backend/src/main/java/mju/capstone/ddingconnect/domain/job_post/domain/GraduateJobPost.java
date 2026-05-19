package mju.capstone.ddingconnect.domain.job_post.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.domain.member.domain.Graduate;
import mju.capstone.ddingconnect.global.common.BaseEntity;

/**
 * [졸업생이 생성한 구직공고 엔티티]
 * ERD 컬럼 매핑:
 * - PK(Long)   → id
 * - PK2(Long)  → graduate (졸업생.PK 참조)
 * - PK3(Long)  → postContents (구직공고.PK 참조)
 *
 * 연결 관계:
 * - 졸업생(Graduate): N:1 (졸업생이 생성한 구직공고.PK2 → 졸업생.PK)
 * - 구직공고(PostContents): N:1 (졸업생이 생성한 구직공고.PK3 → 구직공고.PK)
 */
@Entity
@Table(name = "graduate_job_post")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraduateJobPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graduate_id", nullable = false)
    private Graduate graduate;          // ERD의 PK2 → 졸업생.PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_post_id", nullable = false)
    private PostContents postContents;  // ERD의 PK3 → 구직공고.PK
}

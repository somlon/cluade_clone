package mju.capstone.ddingconnect.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.global.common.BaseEntity;

/**
 * [졸업생 엔티티]
 * ERD 컬럼 매핑:
 * - PK(Long)        → id
 * - FK(Long)        → member (회원.PK 참조)
 * - 명함이미지        → businessCardImage (varchar(255))
 * - 회사명           → company (varchar(255))
 * - 경력(Integer)    → careerYear
 *
 * 연결 관계:
 * - 회원(Member): N:1 (졸업생.FK → 회원.PK)
 * - 졸업생이 생성한 구직공고(GraduateJobPost): 1:N (GraduateJobPost.PK2 → 졸업생.PK)
 */
@Entity
@Table(name = "graduate")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Graduate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;              // ERD의 FK → 회원.PK

    @Column(length = 255)
    private String businessCardImage;   // 명함이미지

    @Column(length = 255)
    private String company;             // 회사명

    private Integer careerYear;          // 경력
}

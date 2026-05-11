package mju.capstone.ddingconnect.domain.techstack.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.common.BaseEntity;

/**
 * [기술 스택 엔티티]
 * ERD 컬럼 매핑:
 * - PK(Long)    → id
 * - FK(Long)    → member (회원.PK 참조)
 * - 이름(ENUM)  → name (TechStackName)
 *
 * 연결 관계:
 * - 회원(Member): N:1 (기술스텍.FK → 회원.PK)
 */
@Entity
@Table(name = "tech_stack")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechStack extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;  // ERD의 FK → 회원.PK

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TechStackName name;  // ERD의 이름(ENUM)
}

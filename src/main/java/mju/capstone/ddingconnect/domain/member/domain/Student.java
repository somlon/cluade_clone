package mju.capstone.ddingconnect.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.global.common.BaseEntity;

/**
 * [재학생 엔티티]
 * ERD 컬럼 매핑:
 * - PK(Long)   → id
 * - FK(Long)   → member (회원.PK 참조)
 * - 학년(Integer) → grade
 *
 * 연결 관계:
 * - 회원(Member): N:1 (재학생.FK → 회원.PK)
 */
@Entity
@Table(name = "student")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;  // ERD의 FK → 회원.PK

    private Integer grade;  // 학년
}

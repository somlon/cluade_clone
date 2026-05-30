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

    /** 학년 허용 범위 — grade 필드의 단일 정의(서비스·가입 OCR 클램프가 공유 참조). */
    public static final int MIN_GRADE = 1;
    public static final int MAX_GRADE = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;  // ERD의 FK → 회원.PK

    private Integer grade;  // 학년

    /**
     * 회원가입 증명서 OCR 로 추출·정규화한 학년을 설정한다(best-effort).
     * 가입 트랜잭션 안에서 막 저장된 영속 엔티티에 호출돼 dirty checking 으로 반영된다.
     */
    public void assignGrade(Integer grade) {
        this.grade = clampGrade(grade);
    }

    /**
     * 학년 값을 유효 범위 {@code [MIN_GRADE, MAX_GRADE]} 로 정규화한다.
     * null 또는 하한 미만 → null, 상한 초과 → 상한으로 클램프.
     * 잘못된 OCR 값이 가입을 막으면 안 되므로 예외를 던지지 않는다
     * (사용자 입력 검증 경로인 {@code MemberServiceImpl.sanitizeGrade} 는 하한 미만을 400 으로 거부 — 정책 분리).
     */
    public static Integer clampGrade(Integer raw) {
        if (raw == null || raw < MIN_GRADE) {
            return null;
        }
        return Math.min(raw, MAX_GRADE);
    }
}

package mju.capstone.ddingconnect.domain.qna.answer.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.domain.member.domain.Member;

/**
 * [답변 좋아요 엔티티]
 * ERD 컬럼 매핑:
 * - PK(Long)   → answer (답변.PK 참조, 복합 PK의 일부)
 * - PK2(Long)  → member (회원.PK 참조, 복합 PK의 일부)
 *
 * 연결 관계:
 * - 답변(Answer): N:1 (답변좋아요.PK → 답변.PK)
 * - 회원(Member): N:1 (답변좋아요.PK2 → 회원.PK)
 *
 * 복합 PK 구성: answer_id + member_id (별도 id 컬럼 없음)
 */
@Entity
@IdClass(AnswerLikeId.class)
@Table(name = "answer_like")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerLike {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private Answer answer;   // ERD의 PK → 답변.PK (복합 키)

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;   // ERD의 PK2 → 회원.PK (복합 키)
}

package mju.capstone.ddingconnect.domain.qna.question.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.common.BaseEntity;

/**
 * [질문 좋아요 엔티티]
 * ERD 컬럼 매핑:
 * - PK(Long)   → id
 * - FK2(Long)  → question (질문.PK 참조)
 * - FK(Long)   → member (회원.PK 참조, 좋아요 누른 회원)
 *
 * 연결 관계:
 * - 질문(Question): N:1 (질문좋아요.FK2 → 질문.PK)
 * - 회원(Member): N:1 (질문좋아요.FK → 회원.PK)
 */
@Entity
@Table(name = "question_like")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;  // ERD의 FK2 → 질문.PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;      // ERD의 FK → 회원.PK (좋아요 누른 회원)
}

package mju.capstone.ddingconnect.domain.qna.answer.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.qna.question.domain.Question;
import mju.capstone.ddingconnect.global.common.BaseEntity;

/**
 * [답변 엔티티]
 * ERD 컬럼 매핑:
 * - PK(Long)          → id
 * - FK(Long)          → question (질문.PK 참조)
 * - PK2(Long)         → member (회원.PK 참조, 답변 작성자)
 * - 내용(varchar(255)) → content
 *
 * 연결 관계:
 * - 질문(Question): N:1 (답변.FK → 질문.PK)
 * - 회원(Member): N:1 (답변.PK2 → 회원.PK)
 * - 답변좋아요(AnswerLike): 1:N (AnswerLike.PK → 답변.PK)
 */
@Entity
@Table(name = "answer")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Answer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;  // ERD의 FK → 질문.PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;      // ERD의 PK2 → 회원.PK (답변 작성자)

    @Column(length = 255)
    private String content;     // 내용
}

package mju.capstone.ddingconnect.domain.qna.question.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.common.BaseEntity;

/**
 * [질문 엔티티]
 * ERD 컬럼 매핑:
 * - PK(Long)              → id
 * - FK(Long)              → member (회원.PK 참조, 질문 작성자)
 * - 카테고리(ENUM)         → category (QuestionCategory)
 * - 제목(varchar(255))     → title
 * - 내용(varchar(255))     → content
 * - 조회수(Integer)        → viewCount
 *
 * 연결 관계:
 * - 회원(Member): N:1 (질문.FK → 회원.PK)
 * - 답변(Answer): 1:N (Answer.FK → 질문.PK)
 * - 질문좋아요(QuestionLike): 1:N (QuestionLike.FK2 → 질문.PK)
 */
@Entity
@Table(name = "question")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;    // ERD의 FK → 회원.PK (질문 작성자)

    @Enumerated(EnumType.STRING)
    private QuestionCategory category;  // 카테고리(ENUM)

    @Column(length = 255)
    private String title;     // 제목

    @Column(length = 255)
    private String content;   // 내용

    private Integer viewCount; // 조회수
}

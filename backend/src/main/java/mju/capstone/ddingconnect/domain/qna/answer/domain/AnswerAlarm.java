package mju.capstone.ddingconnect.domain.qna.answer.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.global.common.BaseEntity;

/**
 * [답변 알람 엔티티]
 * ERD 컬럼 매핑:
 * - PK(Long)           → id
 * - FK(Long)           → answer (답변.PK 참조)
 * - 알람 내용           → content (varchar(255))
 * - 읽음 여부(Boolean)  → isRead
 *
 * 연결 관계:
 * - 답변(Answer): N:1 (답변알람.FK → 답변.PK)
 */
@Entity
@Table(name = "answer_alarm")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerAlarm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private Answer answer;   // ERD의 FK → 답변.PK

    @Column(length = 255)
    private String content;  // 알람 내용

    private Boolean isRead;  // 읽음 여부
}

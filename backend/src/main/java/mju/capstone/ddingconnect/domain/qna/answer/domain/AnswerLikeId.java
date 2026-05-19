package mju.capstone.ddingconnect.domain.qna.answer.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * [답변 좋아요 복합 키 클래스]
 * AnswerLike 엔티티의 복합 PK (answer_id + member_id) 정의
 * - @IdClass 방식으로 AnswerLike에서 참조
 */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AnswerLikeId implements Serializable {

    private Long answer;  // AnswerLike.answer 필드명과 일치
    private Long member;  // AnswerLike.member 필드명과 일치
}

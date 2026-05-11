package mju.capstone.ddingconnect.domain.qna.answer.domain.repository;

import mju.capstone.ddingconnect.domain.qna.answer.domain.AnswerLike;
import mju.capstone.ddingconnect.domain.qna.answer.domain.AnswerLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [답변 좋아요 레포지토리]
 * AnswerLike 엔티티에 대한 데이터베이스 접근 인터페이스
 * - 복합 PK(answer_id + member_id) 사용으로 ID 타입을 AnswerLikeId로 지정
 */
public interface AnswerLikeRepository extends JpaRepository<AnswerLike, AnswerLikeId> {

    long countByAnswerId(Long answerId);

    boolean existsByMemberIdAndAnswerId(Long memberId, Long answerId);
}

package mju.capstone.ddingconnect.domain.qna.question.domain.repository;

import mju.capstone.ddingconnect.domain.qna.question.domain.QuestionLike;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [질문 좋아요 레포지토리]
 * QuestionLike 엔티티에 대한 데이터베이스 접근 인터페이스
 */
public interface QuestionLikeRepository extends JpaRepository<QuestionLike, Long> {

    boolean existsByMemberIdAndQuestionId(Long memberId, Long questionId);

    long countByQuestionId(Long questionId);
}

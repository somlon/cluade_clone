package mju.capstone.ddingconnect.domain.qna.answer.domain.repository;

import mju.capstone.ddingconnect.domain.qna.answer.domain.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * [답변 레포지토리]
 * Answer 엔티티에 대한 데이터베이스 접근 인터페이스
 */
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByQuestionId(Long questionId);

    List<Answer> findByMemberId(Long memberId);

    long countByQuestionId(Long questionId);
}

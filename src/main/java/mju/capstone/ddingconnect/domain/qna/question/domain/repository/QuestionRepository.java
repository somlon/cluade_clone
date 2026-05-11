package mju.capstone.ddingconnect.domain.qna.question.domain.repository;

import mju.capstone.ddingconnect.domain.qna.question.domain.Question;
import mju.capstone.ddingconnect.domain.qna.question.domain.QuestionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * [질문 레포지토리]
 * Question 엔티티에 대한 데이터베이스 접근 인터페이스
 */
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByMemberId(Long memberId);

    List<Question> findByCategory(QuestionCategory category);
}

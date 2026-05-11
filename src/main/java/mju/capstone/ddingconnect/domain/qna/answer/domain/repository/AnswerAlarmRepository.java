package mju.capstone.ddingconnect.domain.qna.answer.domain.repository;

import mju.capstone.ddingconnect.domain.qna.answer.domain.AnswerAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * [답변 알람 레포지토리]
 * AnswerAlarm 엔티티에 대한 데이터베이스 접근 인터페이스
 */
public interface AnswerAlarmRepository extends JpaRepository<AnswerAlarm, Long> {

    List<AnswerAlarm> findByAnswerId(Long answerId);

    List<AnswerAlarm> findByAnswerIdAndIsRead(Long answerId, Boolean isRead);

    long countByAnswerIdAndIsRead(Long answerId, Boolean isRead);

    /**
     * 특정 회원이 작성한 질문에 달린 답변과 연관된 알람을 조회
     * (질문 작성자에게 답변 알람을 보내는 시나리오 가정)
     */
    @Query("SELECT aa FROM AnswerAlarm aa WHERE aa.answer.question.member.id = :memberId")
    List<AnswerAlarm> findByQuestionOwnerId(@Param("memberId") Long memberId);
}

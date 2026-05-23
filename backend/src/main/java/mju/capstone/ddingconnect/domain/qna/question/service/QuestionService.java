package mju.capstone.ddingconnect.domain.qna.question.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.qna.question.dto.request.CreateQuestionRequest;
import mju.capstone.ddingconnect.domain.qna.question.dto.request.UpdateQuestionRequest;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.LikeToggleResponse;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.QuestionResponse;

import java.util.List;

public interface QuestionService {

    QuestionResponse create(Member member, CreateQuestionRequest request);

    List<QuestionResponse> getList(Member member);

    QuestionResponse getOne(Member member, Long questionId);

    QuestionResponse update(Member member, Long questionId, UpdateQuestionRequest request);

    void delete(Member member, Long questionId);

    LikeToggleResponse toggleLike(Member member, Long questionId);

    /** 본인이 작성한 질문 수 */
    long countMyQuestions(Member member);

    /**
     * 본인이 작성한 질문 목록 (나의 활동 페이지 — Q&A 탭).
     * 전체 질문 목록(GET /api/v1/questions)과 분리된 엔드포인트(GET /api/v1/questions/me)가 호출한다.
     */
    List<QuestionResponse> getMyQuestions(Member member);
}

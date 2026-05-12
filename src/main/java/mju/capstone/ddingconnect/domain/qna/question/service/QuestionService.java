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
}

package mju.capstone.ddingconnect.domain.qna.question.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.qna.question.dto.request.CreateQuestionRequest;
import mju.capstone.ddingconnect.domain.qna.question.dto.request.UpdateQuestionRequest;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.QuestionResponse;

import java.util.List;

public interface QuestionService {

    QuestionResponse create(Member member, CreateQuestionRequest request);

    List<QuestionResponse> getList();

    QuestionResponse getOne(Long questionId);

    QuestionResponse update(Member member, Long questionId, UpdateQuestionRequest request);

    void delete(Member member, Long questionId);

    void toggleLike(Member member, Long questionId);
}

package mju.capstone.ddingconnect.domain.qna.answer.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.qna.answer.dto.request.CreateAnswerRequest;
import mju.capstone.ddingconnect.domain.qna.answer.dto.request.UpdateAnswerRequest;
import mju.capstone.ddingconnect.domain.qna.answer.dto.response.AnswerResponse;

import java.util.List;

public interface AnswerService {

    AnswerResponse create(Member member, Long questionId, CreateAnswerRequest request);

    List<AnswerResponse> getList(Long questionId);

    AnswerResponse update(Member member, Long answerId, UpdateAnswerRequest request);

    void delete(Member member, Long answerId);

    void toggleLike(Member member, Long answerId);
}

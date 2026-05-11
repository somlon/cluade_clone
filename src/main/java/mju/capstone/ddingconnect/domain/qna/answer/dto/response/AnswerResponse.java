package mju.capstone.ddingconnect.domain.qna.answer.dto.response;

import mju.capstone.ddingconnect.domain.qna.answer.domain.Answer;

public record AnswerResponse(
        Long id,
        Long questionId,
        Long memberId,
        String content
) {
    public static AnswerResponse from(Answer answer) {
        return new AnswerResponse(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getMember().getId(),
                answer.getContent()
        );
    }
}

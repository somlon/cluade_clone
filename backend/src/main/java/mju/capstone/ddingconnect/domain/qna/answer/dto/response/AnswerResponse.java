package mju.capstone.ddingconnect.domain.qna.answer.dto.response;

import mju.capstone.ddingconnect.domain.qna.answer.domain.Answer;

public record AnswerResponse(
        Long id,
        Long questionId,
        Long memberId,
        String content,
        Long likeCount,
        Boolean likedByMe
) {
    public static AnswerResponse from(Answer answer, long likeCount, boolean likedByMe) {
        return new AnswerResponse(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getMember().getId(),
                answer.getContent(),
                likeCount,
                likedByMe
        );
    }
}

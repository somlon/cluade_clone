package mju.capstone.ddingconnect.domain.qna.question.dto.response;

import mju.capstone.ddingconnect.domain.qna.question.domain.Question;
import mju.capstone.ddingconnect.domain.qna.question.domain.QuestionCategory;

public record QuestionResponse(
        Long id,
        Long memberId,
        QuestionCategory category,
        String title,
        String content,
        Integer viewCount
) {
    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getMember().getId(),
                question.getCategory(),
                question.getTitle(),
                question.getContent(),
                question.getViewCount()
        );
    }
}

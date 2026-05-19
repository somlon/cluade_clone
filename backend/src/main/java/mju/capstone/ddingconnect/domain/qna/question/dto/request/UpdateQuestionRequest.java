package mju.capstone.ddingconnect.domain.qna.question.dto.request;

import mju.capstone.ddingconnect.domain.qna.question.domain.QuestionCategory;

public record UpdateQuestionRequest(
        QuestionCategory category,
        String title,
        String content
) {}

package mju.capstone.ddingconnect.domain.qna.question.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.qna.question.dto.request.CreateQuestionRequest;
import mju.capstone.ddingconnect.domain.qna.question.dto.request.UpdateQuestionRequest;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.QuestionResponse;
import mju.capstone.ddingconnect.domain.qna.question.service.QuestionService;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController implements QuestionSwagger {

    private final QuestionService questionService;

    /** 질문 등록 (Create) */
    @PostMapping
    public ApiResponse<QuestionResponse> createQuestion(
            @Parameter(hidden = true) @LoginMember Member member,
            @RequestBody CreateQuestionRequest request) {
        return ApiResponse.onSuccess(questionService.create(member, request));
    }

    /** 질문 목록 조회 (Read) */
    @GetMapping
    public ApiResponse<List<QuestionResponse>> getQuestions() {
        return ApiResponse.onSuccess(questionService.getList());
    }

    /** 질문 상세 조회 (Read - 질문 카드를 클릭했을 때 본문 + 답변 화면 진입 시 호출, 조회수 +1 증가) */
    @GetMapping("/{questionId}")
    public ApiResponse<QuestionResponse> getQuestion(@PathVariable Long questionId) {
        return ApiResponse.onSuccess(questionService.getOne(questionId));
    }

    /** 질문 수정 (Update) */
    @PatchMapping("/{questionId}")
    public ApiResponse<QuestionResponse> updateQuestion(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long questionId,
            @RequestBody UpdateQuestionRequest request) {
        return ApiResponse.onSuccess(questionService.update(member, questionId, request));
    }

    /** 질문 삭제 (Delete) */
    @DeleteMapping("/{questionId}")
    public ApiResponse<String> deleteQuestion(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long questionId) {
        questionService.delete(member, questionId);
        return ApiResponse.onSuccess("질문이 삭제되었습니다.");
    }

    /** 질문 좋아요 토글 */
    @PostMapping("/{questionId}/like")
    public ApiResponse<String> likeQuestion(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long questionId) {
        questionService.toggleLike(member, questionId);
        return ApiResponse.onSuccess("좋아요가 처리되었습니다.");
    }
}

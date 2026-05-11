package mju.capstone.ddingconnect.domain.qna.answer.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.qna.answer.dto.request.CreateAnswerRequest;
import mju.capstone.ddingconnect.domain.qna.answer.dto.request.UpdateAnswerRequest;
import mju.capstone.ddingconnect.domain.qna.answer.dto.response.AnswerResponse;
import mju.capstone.ddingconnect.domain.qna.answer.service.AnswerService;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions/{questionId}/answers")
@RequiredArgsConstructor
public class AnswerController implements AnswerSwagger {

    private final AnswerService answerService;

    /** 답변 등록 (Create) */
    @PostMapping
    public ApiResponse<AnswerResponse> createAnswer(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long questionId,
            @RequestBody CreateAnswerRequest request) {
        return ApiResponse.onSuccess(answerService.create(member, questionId, request));
    }

    /** 답변 목록 조회 (Read) */
    @GetMapping
    public ApiResponse<List<AnswerResponse>> getAnswers(@PathVariable Long questionId) {
        return ApiResponse.onSuccess(answerService.getList(questionId));
    }

    /** 답변 수정 (Update) */
    @PatchMapping("/{answerId}")
    public ApiResponse<AnswerResponse> updateAnswer(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long questionId,
            @PathVariable Long answerId,
            @RequestBody UpdateAnswerRequest request) {
        return ApiResponse.onSuccess(answerService.update(member, answerId, request));
    }

    /** 답변 삭제 (Delete) */
    @DeleteMapping("/{answerId}")
    public ApiResponse<String> deleteAnswer(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long questionId,
            @PathVariable Long answerId) {
        answerService.delete(member, answerId);
        return ApiResponse.onSuccess("답변이 삭제되었습니다.");
    }

    /** 답변 좋아요 토글 */
    @PostMapping("/{answerId}/like")
    public ApiResponse<String> likeAnswer(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long questionId,
            @PathVariable Long answerId) {
        answerService.toggleLike(member, answerId);
        return ApiResponse.onSuccess("좋아요가 처리되었습니다.");
    }
}

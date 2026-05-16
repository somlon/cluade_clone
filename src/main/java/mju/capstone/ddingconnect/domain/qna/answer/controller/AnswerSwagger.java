package mju.capstone.ddingconnect.domain.qna.answer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.qna.answer.dto.request.CreateAnswerRequest;
import mju.capstone.ddingconnect.domain.qna.answer.dto.request.UpdateAnswerRequest;
import mju.capstone.ddingconnect.domain.qna.answer.dto.response.AnswerResponse;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.LikeToggleResponse;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "답변", description = "Q&A 답변 컨트롤러 — 특정 질문 하위 답변의 등록/조회/수정/삭제 CRUD 및 좋아요 토글 엔드포인트를 제공합니다.")
public interface AnswerSwagger {

    @Operation(
            summary = "답변 등록",
            description = "특정 질문에 새로운 답변을 등록합니다. **졸업생** 또는 **질문 작성자 본인**만 등록 가능합니다 (그 외 역할 → 403)."
    )
    @PostMapping
    ApiResponse<AnswerResponse> createAnswer(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "답변을 등록할 질문 ID")
            @PathVariable Long questionId,
            @Parameter(description = "답변 등록 정보 (본문 등)")
            @RequestBody CreateAnswerRequest request);




    @Operation(
            summary = "답변 목록 조회",
            description = "특정 질문에 달린 모든 답변 목록을 조회합니다. 각 항목에 좋아요 개수(`likeCount`), 본인 좋아요 여부(`likedByMe`)가 포함됩니다."
    )
    @GetMapping
    ApiResponse<List<AnswerResponse>> getAnswers(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "답변 목록을 조회할 질문 ID")
            @PathVariable Long questionId);




    @Operation(
            summary = "답변 수정",
            description = "본인이 작성한 답변의 내용을 수정합니다."
    )
    @PatchMapping("/{answerId}")
    ApiResponse<AnswerResponse> updateAnswer(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "답변이 속한 질문 ID")
            @PathVariable Long questionId,
            @Parameter(description = "수정할 답변 ID")
            @PathVariable Long answerId,
            @Parameter(description = "답변 수정 정보")
            @RequestBody UpdateAnswerRequest request);




    @Operation(
            summary = "답변 삭제",
            description = "본인이 작성한 답변을 삭제합니다."
    )
    @DeleteMapping("/{answerId}")
    ApiResponse<String> deleteAnswer(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "답변이 속한 질문 ID")
            @PathVariable Long questionId,
            @Parameter(description = "삭제할 답변 ID")
            @PathVariable Long answerId);




    @Operation(
            summary = "답변 좋아요 토글",
            description = "특정 답변에 좋아요를 누르거나 취소합니다. 이미 좋아요를 누른 상태라면 좋아요가 취소됩니다. 본인이 작성한 답변에는 좋아요 불가(400). 토글 후 새 상태(`liked`)와 카운트(`likeCount`)가 응답에 포함됩니다."
    )
    @PostMapping("/{answerId}/like")
    ApiResponse<LikeToggleResponse> likeAnswer(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "답변이 속한 질문 ID")
            @PathVariable Long questionId,
            @Parameter(description = "좋아요를 토글할 답변 ID")
            @PathVariable Long answerId);

}

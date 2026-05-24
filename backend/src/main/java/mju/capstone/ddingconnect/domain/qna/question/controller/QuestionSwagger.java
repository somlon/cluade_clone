package mju.capstone.ddingconnect.domain.qna.question.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.qna.question.dto.request.CreateQuestionRequest;
import mju.capstone.ddingconnect.domain.qna.question.dto.request.UpdateQuestionRequest;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.LikeToggleResponse;
import mju.capstone.ddingconnect.domain.qna.question.dto.response.QuestionResponse;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "질문", description = "Q&A 질문 컨트롤러 — 등록/조회/수정/삭제 CRUD 및 좋아요 토글 엔드포인트를 제공합니다.")
public interface QuestionSwagger {

    @Operation(
            summary = "질문 등록",
            description = "Q&A 게시판에 새로운 질문을 등록합니다. 로그인된 회원만 호출할 수 있습니다."
    )
    @PostMapping
    ApiResponse<QuestionResponse> createQuestion(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "질문 등록 정보 (제목, 본문 등)")
            @RequestBody CreateQuestionRequest request);




    @Operation(
            summary = "질문 목록 조회",
            description = "등록된 모든 질문 목록을 조회합니다. 각 항목에 좋아요 개수(`likeCount`), 답변 개수(`answerCount`), 본인 좋아요 여부(`likedByMe`)가 포함됩니다."
    )
    @GetMapping
    ApiResponse<List<QuestionResponse>> getQuestions(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "나의 질문 목록 조회 (나의 활동 페이지)",
            description = "로그인한 회원이 작성한 질문만 조회합니다. 마이페이지의 '질문 수' 카드를 클릭해 진입하는 '나의 활동/Q&A' 화면용. "
                    + "전체 질문 목록(GET /api/v1/questions)과 분리된 엔드포인트로, 응답 DTO 는 동일합니다."
    )
    @Tag(name = "나의 활동")
    @GetMapping("/me")
    ApiResponse<List<QuestionResponse>> getMyQuestions(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "질문 상세 조회",
            description = "질문 카드를 클릭했을 때 본문과 답변 화면 진입 시 호출되며, 조회수가 +1 증가합니다. 응답에 좋아요 개수(`likeCount`), 답변 개수(`answerCount`), 본인 좋아요 여부(`likedByMe`)가 포함됩니다."
    )
    @GetMapping("/{questionId}")
    ApiResponse<QuestionResponse> getQuestion(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "조회할 질문 ID")
            @PathVariable Long questionId);




    @Operation(
            summary = "질문 수정",
            description = "본인이 작성한 질문의 일부 또는 전체 정보를 수정합니다."
    )
    @PatchMapping("/{questionId}")
    ApiResponse<QuestionResponse> updateQuestion(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "수정할 질문 ID")
            @PathVariable Long questionId,
            @Parameter(description = "질문 수정 정보")
            @RequestBody UpdateQuestionRequest request);




    @Operation(
            summary = "질문 삭제",
            description = "본인이 작성한 질문을 삭제합니다."
    )
    @DeleteMapping("/{questionId}")
    ApiResponse<String> deleteQuestion(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "삭제할 질문 ID")
            @PathVariable Long questionId);




    @Operation(
            summary = "질문 좋아요 토글",
            description = "특정 질문에 좋아요를 누르거나 취소합니다. 이미 좋아요를 누른 상태라면 좋아요가 취소됩니다. 본인이 작성한 질문에는 좋아요 불가(400). 토글 후 새 상태(`liked`)와 카운트(`likeCount`)가 응답에 포함됩니다."
    )
    @PostMapping("/{questionId}/like")
    ApiResponse<LikeToggleResponse> likeQuestion(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "좋아요를 토글할 질문 ID")
            @PathVariable Long questionId);

}

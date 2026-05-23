package mju.capstone.ddingconnect.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateGraduateMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMemberRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateStudentMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.MyPageResponse;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "회원", description = "회원 컨트롤러 — 프로필 조회/수정/탈퇴(소프트 삭제), 마이페이지 조회 엔드포인트를 제공합니다. (회원가입은 인증 API에서 담당)")
public interface MemberSwagger {

    @Operation(
            summary = "JWT 인증 테스트",
            description = "JWT 토큰 인증이 정상적으로 동작하는지 확인하는 테스트용 엔드포인트입니다. 로그인된 회원의 이메일을 반환합니다.",
            hidden = true
    )
    @GetMapping("/test")
    ApiResponse<String> test(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "내 프로필 조회",
            description = "로그인된 회원의 프로필 정보를 조회합니다."
    )
    @GetMapping("/me")
    ApiResponse<MemberResponse> getMyProfile(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "회원 정보 수정",
            description = "로그인된 회원의 프로필 정보(닉네임, 자기소개 등)를 수정합니다."
    )
    @PatchMapping("/me")
    ApiResponse<MemberResponse> updateMyProfile(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "수정할 회원 정보")
            @RequestBody UpdateMemberRequest request);




    @Operation(
            summary = "회원 탈퇴",
            description = "로그인된 회원의 계정을 소프트 삭제(비활성화) 방식으로 탈퇴 처리합니다."
    )
    @DeleteMapping("/me")
    ApiResponse<String> withdraw(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "마이페이지 조회",
            description = "로그인된 회원의 마이페이지 정보를 한 번에 조회합니다. "
                    + "프로필, 활동 통계(커피챗/로드맵/질문 수), 기술 스택과 "
                    + "역할별 항목(재학생: 관심 직군, 졸업생: 등록 구직 공고)을 포함합니다."
    )
    @GetMapping("/mypage")
    ApiResponse<MyPageResponse> getMyPage(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "재학생 마이페이지 통합 수정",
            description = "STUDENT 역할 전용 마이페이지 편집 항목을 1회 요청으로 일괄 수정합니다. "
                    + "프로필(공통 9필드 + 학년), 기술 스택, 관심 직군을 포함합니다. "
                    + "STUDENT 가 아니면 400(MEMBER_FIELD_ROLE_MISMATCH)으로 거부합니다(UNKNOWN/GRADUATE 모두 거부). "
                    + "단일 트랜잭션으로 처리되어 일부라도 실패하면 전체가 롤백됩니다. "
                    + "각 항목은 미전송(null) 시 변경하지 않으며, 수정 후 최신 마이페이지를 반환합니다."
    )
    @PatchMapping("/mypage/student")
    ApiResponse<MyPageResponse> updateStudentMyPage(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "재학생 마이페이지 통합 수정 요청")
            @RequestBody UpdateStudentMyPageRequest request);




    @Operation(
            summary = "졸업생 마이페이지 통합 수정",
            description = "GRADUATE 역할 전용 마이페이지 편집 항목을 1회 요청으로 일괄 수정합니다. "
                    + "프로필(공통 9필드 + 명함이미지/직무/회사명/경력), 기술 스택, "
                    + "구직 공고 링크 추가·삭제를 포함합니다. "
                    + "GRADUATE 가 아니면 400(MEMBER_FIELD_ROLE_MISMATCH)으로 거부합니다(UNKNOWN/STUDENT 모두 거부). "
                    + "단일 트랜잭션으로 처리되어 일부라도 실패하면 전체가 롤백됩니다. "
                    + "각 항목은 미전송(null) 시 변경하지 않으며, 수정 후 최신 마이페이지를 반환합니다."
    )
    @PatchMapping("/mypage/graduate")
    ApiResponse<MyPageResponse> updateGraduateMyPage(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "졸업생 마이페이지 통합 수정 요청")
            @RequestBody UpdateGraduateMyPageRequest request);




    @Operation(
            summary = "프로필 사진 업로드/교체",
            description = "마이페이지 상단 프로필 사진(동그라미)을 업로드해 교체합니다. "
                    + "multipart/form-data 로 `image` 파트에 파일을 전송합니다. "
                    + "허용 content-type: image/png, image/jpeg, image/webp. 크기 제한: 5MB. "
                    + "위반 시 400(`_FILE_TYPE_NOT_ALLOWED` / `_FILE_TOO_LARGE`)으로 거부합니다. "
                    + "기존 프로필 사진이 있으면 S3 에서 먼저 삭제한 후 새 사진을 업로드합니다. "
                    + "응답은 갱신된 회원 정보(`MemberResponse`) 입니다."
    )
    @PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<MemberResponse> updateProfileImage(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "업로드할 프로필 이미지 파일 (image/png, image/jpeg, image/webp)")
            @RequestPart("image") MultipartFile image);

}

package mju.capstone.ddingconnect.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateGraduateMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMemberRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateStudentMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.HomeResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.MyPageResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.PointBalanceResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.PointChargeResponse;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadRequest;
import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadResponse;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

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
            summary = "홈 화면 조회",
            description = "로그인된 회원의 홈 화면 정보를 한 번에 조회합니다. "
                    + "보유 포인트(P), 닉네임, 역할별 학과/학년 자리 표시 필드, 활동 카운트(커피챗/로드맵/QnA)를 포함합니다. "
                    + "학과 자리: STUDENT 는 department, GRADUATE 는 company. "
                    + "학년 자리: STUDENT 는 grade, GRADUATE 는 careerYear. "
                    + "비해당 역할 필드(STUDENT 의 company/careerYear, GRADUATE 의 department/grade, "
                    + "GRADUATE 의 activity.roadmapCount)는 응답에서 키 자체가 제외됩니다. "
                    + "활동 카운트는 마이페이지와 동일 집계 기준을 재사용합니다."
    )
    @GetMapping("/me/home")
    ApiResponse<HomeResponse> getHome(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "포인트 잔액 조회",
            description = "로그인된 회원의 현재 보유 포인트(P)만 조회합니다. "
                    + "홈 화면에서 우측 상단 포인트 뱃지를 눌렀을 때 뜨는 모달에서 사용합니다. "
                    + "충전 직후 등 최신 P 값이 필요한 시점에 호출하세요."
    )
    @GetMapping("/me/point")
    ApiResponse<PointBalanceResponse> getMyPoint(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "포인트 충전 페이지 데이터 조회",
            description = "포인트 충전 페이지 렌더링용 통합 응답을 반환합니다. "
                    + "보유 포인트(P)와 충전 상품 카드 리스트(상품 id, 충전 P, 결제 금액(원), 추천 뱃지 여부)를 함께 제공합니다. "
                    + "상품 목록은 서버 상수(PointProduct enum)로 관리되며 단위는 P로 통일됩니다 (프론트의 '토큰' 라벨은 동일 값을 다른 라벨로 표기)."
    )
    @GetMapping("/me/point/products")
    ApiResponse<PointChargeResponse> getPointProducts(
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
            summary = "프로필 사진 업로드용 presigned URL 발급",
            description = "마이페이지 프로필 사진을 교체하기 위한 presigned PUT URL 을 발급합니다(로그인 필수). "
                    + "요청 본문 `{fileName, contentType}`, 허용 content-type: image/png, image/jpeg, image/webp. "
                    + "비허용 content-type 은 400(S3400)으로 거부됩니다.\n\n"
                    + "2-step 플로우: ① 발급받은 `uploadUrl` 에 파일 바이트를 PUT(헤더 Content-Type 은 요청의 contentType 과 동일) "
                    + "→ ② 업로드 성공 후 `fileUrl` 을 `PATCH /api/v1/members/me` 의 profileImage 필드에 저장합니다. "
                    + "presigned PUT 은 서버측 크기 강제가 불가하므로 파일 크기 제한은 프론트에서 검증합니다."
    )
    @PostMapping("/me/profile-image/presigned-url")
    ApiResponse<PresignedUploadResponse> createProfileImageUploadUrl(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "업로드 요청 (fileName, contentType)")
            @Valid @RequestBody PresignedUploadRequest request);




    @Operation(
            summary = "졸업생 명함 사진 업로드용 presigned URL 발급 (GRADUATE 전용)",
            description = "졸업생 마이페이지의 명함 사진을 교체하기 위한 presigned PUT URL 을 발급합니다(로그인 필수). "
                    + "GRADUATE 가 아니면 400(`MEMBER_FIELD_ROLE_MISMATCH`)으로 거부합니다(STUDENT/UNKNOWN 모두 거부). "
                    + "허용 content-type: image/png, image/jpeg, image/webp.\n\n"
                    + "2-step 플로우: ① 발급받은 `uploadUrl` 에 파일 바이트 PUT → ② `fileUrl` 을 "
                    + "`PATCH /api/v1/members/mypage/graduate` 의 businessCardImage 필드에 저장합니다."
    )
    @PostMapping("/me/business-card/presigned-url")
    ApiResponse<PresignedUploadResponse> createBusinessCardUploadUrl(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "업로드 요청 (fileName, contentType)")
            @Valid @RequestBody PresignedUploadRequest request);




    @Operation(
            summary = "포트폴리오 파일 업로드용 presigned URL 발급",
            description = "마이페이지 포트폴리오 파일을 교체하기 위한 presigned PUT URL 을 발급합니다(로그인 필수). "
                    + "허용 content-type: application/pdf. 비허용 content-type 은 400(S3400)으로 거부됩니다.\n\n"
                    + "2-step 플로우: ① 발급받은 `uploadUrl` 에 파일 바이트 PUT → ② `fileUrl` 을 "
                    + "`PATCH /api/v1/members/me` 의 portfolio 필드에 저장합니다."
    )
    @PostMapping("/me/portfolio/presigned-url")
    ApiResponse<PresignedUploadResponse> createPortfolioUploadUrl(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "업로드 요청 (fileName, contentType)")
            @Valid @RequestBody PresignedUploadRequest request);




    @Operation(
            summary = "프로필 사진 삭제",
            description = "로그인된 회원의 프로필 사진을 삭제합니다. S3 객체를 best-effort 로 삭제한 뒤 "
                    + "`Member.profileImage` 를 null 로 갱신합니다. 이미 비어 있으면 변경 없이 현재 프로필을 그대로 반환합니다(idempotent, 200). "
                    + "S3 객체 삭제가 실패해도 DB 클리어는 그대로 진행됩니다(로그만 기록). 갱신된 회원 프로필을 반환합니다."
    )
    @DeleteMapping("/me/profile-image")
    ApiResponse<MemberResponse> deleteProfileImage(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "졸업생 명함 사진 삭제 (GRADUATE 전용)",
            description = "로그인된 졸업생의 명함 사진을 삭제합니다. S3 객체를 best-effort 로 삭제한 뒤 "
                    + "`Graduate.businessCardImage` 를 null 로 갱신합니다. "
                    + "GRADUATE 가 아니면 400(`MEMBER_FIELD_ROLE_MISMATCH`)으로 거부합니다(STUDENT/UNKNOWN 모두 거부). "
                    + "이미 비어 있으면 변경 없이 현재 프로필을 그대로 반환합니다(idempotent, 200)."
    )
    @DeleteMapping("/me/business-card")
    ApiResponse<MemberResponse> deleteBusinessCard(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "포트폴리오 삭제",
            description = "로그인된 회원의 포트폴리오 파일을 삭제합니다. S3 객체를 best-effort 로 삭제한 뒤 "
                    + "`Member.portfolio` 를 null 로 갱신합니다. 이미 비어 있으면 변경 없이 현재 프로필을 그대로 반환합니다(idempotent, 200). "
                    + "S3 객체 삭제가 실패해도 DB 클리어는 그대로 진행됩니다(로그만 기록)."
    )
    @DeleteMapping("/me/portfolio")
    ApiResponse<MemberResponse> deletePortfolio(
            @Parameter(hidden = true) @LoginMember Member member);

}

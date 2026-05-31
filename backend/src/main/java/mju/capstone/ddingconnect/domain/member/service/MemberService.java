package mju.capstone.ddingconnect.domain.member.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMemberRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadRequest;
import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadResponse;

public interface MemberService {

    /** 내 프로필 조회 */
    MemberResponse getMyProfile(Member member);

    /** 회원 정보 수정 */
    MemberResponse updateMyProfile(Member member, UpdateMemberRequest request);

    /** 회원 탈퇴 (소프트 삭제) */
    void withdraw(Member member);

    /**
     * 프로필 사진 업로드용 presigned PUT URL 발급.
     * - content-type 화이트리스트: image/png, image/jpeg, image/webp ({@code UploadType.IMAGE}) — 위반 시 _FILE_TYPE_NOT_ALLOWED(400)
     * - 발급만 수행(2-step): 프론트가 받은 uploadUrl 로 S3 에 직접 PUT 한 뒤, fileUrl 을 {@code PATCH /me} 의 profileImage 필드에 저장한다.
     */
    PresignedUploadResponse createProfileImageUploadUrl(PresignedUploadRequest request);

    /**
     * 졸업생 명함 사진 업로드용 presigned PUT URL 발급 (GRADUATE 전용).
     * - 진입 가드: STUDENT/UNKNOWN 호출 시 MEMBER_FIELD_ROLE_MISMATCH 로 거부
     * - content-type 화이트리스트: image/png, image/jpeg, image/webp ({@code UploadType.IMAGE}) — 위반 시 _FILE_TYPE_NOT_ALLOWED(400)
     * - 발급만 수행(2-step): fileUrl 을 {@code PATCH /mypage/graduate} 의 businessCardImage 필드에 저장한다.
     */
    PresignedUploadResponse createBusinessCardUploadUrl(Member member, PresignedUploadRequest request);

    /**
     * 포트폴리오 파일 업로드용 presigned PUT URL 발급.
     * - content-type 화이트리스트: application/pdf ({@code UploadType.PORTFOLIO}) — 위반 시 _FILE_TYPE_NOT_ALLOWED(400)
     * - 발급만 수행(2-step): fileUrl 을 {@code PATCH /me} 의 portfolio 필드에 저장한다.
     */
    PresignedUploadResponse createPortfolioUploadUrl(PresignedUploadRequest request);
}

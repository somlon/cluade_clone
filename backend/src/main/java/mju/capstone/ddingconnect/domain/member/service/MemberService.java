package mju.capstone.ddingconnect.domain.member.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMemberRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import org.springframework.web.multipart.MultipartFile;

public interface MemberService {

    /** 내 프로필 조회 */
    MemberResponse getMyProfile(Member member);

    /** 회원 정보 수정 */
    MemberResponse updateMyProfile(Member member, UpdateMemberRequest request);

    /** 회원 탈퇴 (소프트 삭제) */
    void withdraw(Member member);

    /**
     * 프로필 이미지 업로드/교체.
     * - content-type 화이트리스트: image/png, image/jpeg, image/webp (위반 시 _FILE_TYPE_NOT_ALLOWED)
     * - 크기 제한: 5MB (위반 시 _FILE_TOO_LARGE)
     * - 기존 profileImage URL 이 있으면 S3 에서 삭제 후 새 이미지 업로드 → Member.profileImage 갱신
     */
    MemberResponse updateProfileImage(Member member, MultipartFile image);

    /**
     * 졸업생 명함 이미지 업로드/교체 (GRADUATE 전용).
     * - 진입 가드: STUDENT/UNKNOWN 호출 시 MEMBER_FIELD_ROLE_MISMATCH 로 거부
     * - content-type 화이트리스트: image/png, image/jpeg, image/webp (위반 시 _FILE_TYPE_NOT_ALLOWED)
     * - 크기 제한: 5MB (위반 시 _FILE_TOO_LARGE)
     * - 기존 businessCardImage URL 이 있으면 S3 에서 삭제 후 새 이미지 업로드 → Graduate.businessCardImage 갱신
     */
    MemberResponse updateBusinessCard(Member member, MultipartFile image);

    /**
     * 포트폴리오 PDF 업로드/교체.
     * - content-type: application/pdf (위반 시 _FILE_TYPE_NOT_ALLOWED)
     * - 크기 제한: 20MB (위반 시 _FILE_TOO_LARGE)
     * - 기존 portfolio URL 이 있으면 S3 에서 삭제 후 새 PDF 업로드 → Member.portfolio 갱신
     */
    MemberResponse updatePortfolio(Member member, MultipartFile file);
}

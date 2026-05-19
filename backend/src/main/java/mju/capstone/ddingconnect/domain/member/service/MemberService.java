package mju.capstone.ddingconnect.domain.member.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMemberRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;

public interface MemberService {

    /** 내 프로필 조회 */
    MemberResponse getMyProfile(Member member);

    /** 회원 정보 수정 */
    MemberResponse updateMyProfile(Member member, UpdateMemberRequest request);

    /** 회원 탈퇴 (소프트 삭제) */
    void withdraw(Member member);
}

package mju.capstone.ddingconnect.domain.member.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MyPageResponse;

public interface MyPageService {

    /** 마이페이지 조회 — 항목별 도메인 서비스의 조회 결과를 한 응답으로 조합한다. */
    MyPageResponse getMyPage(Member member);

    /** 마이페이지 통합 수정 — 항목별 도메인 수정 API 에 위임하고 수정 후 최신 마이페이지를 반환한다. */
    MyPageResponse updateMyPage(Member member, UpdateMyPageRequest request);
}

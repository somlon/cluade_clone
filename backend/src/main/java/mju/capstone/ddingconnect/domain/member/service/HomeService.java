package mju.capstone.ddingconnect.domain.member.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.dto.response.HomeResponse;

public interface HomeService {

    /**
     * 홈 화면 조회 — 포인트/닉네임/학과/학년(또는 경력)/활동 카운트를 한 응답으로 조합한다.
     * 활동 카운트는 마이페이지(MyPageService)와 동일 집계 기준의 도메인 서비스 메서드를 그대로 재사용한다.
     */
    HomeResponse getHome(Member member);
}

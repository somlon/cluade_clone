package mju.capstone.ddingconnect.domain.member.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateGraduateMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateStudentMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MyPageResponse;

public interface MyPageService {

    /** 마이페이지 조회 — 항목별 도메인 서비스의 조회 결과를 한 응답으로 조합한다. */
    MyPageResponse getMyPage(Member member);

    /**
     * 재학생 마이페이지 통합 수정.
     * STUDENT 전용 항목(targetJobs)과 공통 항목(profile, techStacks)을 1회 요청으로 일괄 수정한다.
     * 진입부에서 STUDENT 가 아니면 MEMBER_FIELD_ROLE_MISMATCH 로 거부.
     */
    MyPageResponse updateStudentMyPage(Member member, UpdateStudentMyPageRequest request);

    /**
     * 졸업생 마이페이지 통합 수정.
     * GRADUATE 전용 항목(jobPostsToAdd·jobPostIdsToDelete)과 공통 항목(profile, techStacks)을 1회 요청으로 일괄 수정한다.
     * 진입부에서 GRADUATE 가 아니면 MEMBER_FIELD_ROLE_MISMATCH 로 거부.
     */
    MyPageResponse updateGraduateMyPage(Member member, UpdateGraduateMyPageRequest request);
}

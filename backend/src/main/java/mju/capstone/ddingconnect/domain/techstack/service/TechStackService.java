package mju.capstone.ddingconnect.domain.techstack.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.techstack.dto.request.ReplaceTechStackRequest;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;

import java.util.List;

public interface TechStackService {

    /** 기술 스택 일괄 교체 (REPLACE) */
    List<TechStackResponse> replace(Member member, ReplaceTechStackRequest request);

    /** 내 기술 스택 목록 조회 */
    List<TechStackResponse> getMyTechStacks(Member member);
}

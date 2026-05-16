package mju.capstone.ddingconnect.domain.interested_job.service;

import mju.capstone.ddingconnect.domain.interested_job.dto.request.ReplaceTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
import mju.capstone.ddingconnect.domain.member.domain.Member;

import java.util.List;

public interface TargetJobService {

    /** 관심 직군 일괄 교체 (REPLACE) */
    List<TargetJobResponse> replace(Member member, ReplaceTargetJobRequest request);

    /** 내 관심 직군 목록 조회 */
    List<TargetJobResponse> getMyTargetJobs(Member member);
}

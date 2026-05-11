package mju.capstone.ddingconnect.domain.interested_job.service;

import mju.capstone.ddingconnect.domain.interested_job.dto.request.CreateTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.UpdateTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
import mju.capstone.ddingconnect.domain.member.domain.Member;

import java.util.List;

public interface TargetJobService {

    /** 관심 직군 추가 */
    TargetJobResponse create(Member member, CreateTargetJobRequest request);

    /** 내 관심 직군 목록 조회 */
    List<TargetJobResponse> getMyTargetJobs(Member member);

    /** 관심 직군 수정 */
    TargetJobResponse update(Member member, Long targetJobId, UpdateTargetJobRequest request);

    /** 관심 직군 삭제 */
    void delete(Member member, Long targetJobId);
}

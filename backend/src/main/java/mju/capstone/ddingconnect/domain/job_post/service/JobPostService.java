package mju.capstone.ddingconnect.domain.job_post.service;

import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostLinkRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.request.UpdateJobPostRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.response.JobPostResponse;
import mju.capstone.ddingconnect.domain.member.domain.Member;

import java.util.List;

public interface JobPostService {

    /** 구직 공고 등록 (졸업생 전용) */
    JobPostResponse create(Member member, CreateJobPostRequest request);

    /**
     * 구직 공고 링크 전용 등록 (졸업생 전용).
     * detailUrl 만 채워 저장하고 알람 분기는 스킵한다(jobType 이 null 이라 관심직무 매칭 불가).
     * 마이페이지 졸업생 통합 수정의 jobPostsToAdd 위임 진입점.
     */
    JobPostResponse createFromLink(Member member, CreateJobPostLinkRequest request);

    /** 구직 공고 목록 조회 */
    List<JobPostResponse> getList();

    /** 구직 공고 단건 조회 */
    JobPostResponse getOne(Long jobPostId);

    /** 구직 공고 수정 */
    JobPostResponse update(Member member, Long jobPostId, UpdateJobPostRequest request);

    /** 구직 공고 삭제 */
    void delete(Member member, Long jobPostId);

    /** 본인(졸업생)이 등록한 구직 공고 목록 */
    List<JobPostResponse> getMyJobPosts(Member member);
}

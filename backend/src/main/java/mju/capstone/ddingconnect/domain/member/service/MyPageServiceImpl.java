package mju.capstone.ddingconnect.domain.member.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.coffeechat.service.CoffeeChatService;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.ReplaceTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
import mju.capstone.ddingconnect.domain.interested_job.service.TargetJobService;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.response.JobPostResponse;
import mju.capstone.ddingconnect.domain.job_post.service.JobPostService;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.MyPageResponse;
import mju.capstone.ddingconnect.domain.qna.question.service.QuestionService;
import mju.capstone.ddingconnect.domain.roadmap.service.RoadmapService;
import mju.capstone.ddingconnect.domain.techstack.dto.request.ReplaceTechStackRequest;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;
import mju.capstone.ddingconnect.domain.techstack.service.TechStackService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * [마이페이지 서비스 구현체]
 * 항목별 도메인 서비스를 in-process 로 조합하는 애그리게이터.
 * 자체 레포지토리 의존 없이 각 도메인 서비스만 호출한다 (조회·수정 모두 동일 패턴).
 */
@Service
@RequiredArgsConstructor
public class MyPageServiceImpl implements MyPageService {

    private final MemberService memberService;
    private final TechStackService techStackService;
    private final TargetJobService targetJobService;
    private final CoffeeChatService coffeeChatService;
    private final RoadmapService roadmapService;
    private final QuestionService questionService;
    private final JobPostService jobPostService;

    @Override
    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(Member member) {
        return buildResponse(member, memberService.getMyProfile(member));
    }

    /**
     * 마이페이지 통합 수정.
     * 항목별로 분해해 각 도메인의 기존 수정 API 에 위임하고, 수정 후 최신 마이페이지를 반환한다.
     * 단일 @Transactional 안에서 모든 위임이 실행되므로 일부라도 실패하면 전체가 롤백된다('수정 완료' 원자성).
     * 위임 메서드는 모두 @Transactional(전파 REQUIRED)이라 이 트랜잭션에 참여한다.
     * 각 항목은 null 이면 변경하지 않는다(부분 수정).
     */
    @Override
    @Transactional
    public MyPageResponse updateMyPage(Member member, UpdateMyPageRequest request) {
        // updateMyProfile 이 반환한 MemberResponse 를 그대로 응답에 쓴다.
        // 전달된 member 객체는 수정으로 갱신되지 않아, 다시 조회하면 공통 필드가 stale 하기 때문.
        MemberResponse profile = request.profile() != null
                ? memberService.updateMyProfile(member, request.profile())
                : memberService.getMyProfile(member);

        if (request.techStacks() != null) {
            techStackService.replace(member, new ReplaceTechStackRequest(request.techStacks()));
        }

        if (request.targetJobs() != null) {
            targetJobService.replace(member, new ReplaceTargetJobRequest(request.targetJobs()));
        }

        // 졸업생 구직 공고는 삭제 후 추가. 졸업생 권한·소유자 검증은 위임 메서드(JobPostService)가 수행한다.
        if (request.jobPostIdsToDelete() != null) {
            for (Long jobPostId : request.jobPostIdsToDelete()) {
                jobPostService.delete(member, jobPostId);
            }
        }
        if (request.jobPostsToAdd() != null) {
            for (CreateJobPostRequest toAdd : request.jobPostsToAdd()) {
                jobPostService.create(member, toAdd);
            }
        }

        return buildResponse(member, profile);
    }

    /**
     * profile 을 제외한 마이페이지 구성 요소(활동 통계/기술 스택/역할별 항목)를 조회해 한 응답으로 조합한다.
     * getMyPage(조회)와 updateMyPage(수정 후 최신 조회)가 공유한다.
     */
    private MyPageResponse buildResponse(Member member, MemberResponse profile) {
        MyPageResponse.ActivityStats activity = new MyPageResponse.ActivityStats(
                coffeeChatService.countMyAcceptedCoffeeChats(member),
                roadmapService.countMyRoadmaps(member),
                questionService.countMyQuestions(member)
        );

        List<TechStackResponse> techStacks = techStackService.getMyTechStacks(member);

        // 관심 직군은 재학생 마이페이지 항목, 등록 구직 공고는 졸업생 마이페이지 항목
        List<TargetJobResponse> targetJobs = member.getRole() == MemberRole.STUDENT
                ? targetJobService.getMyTargetJobs(member)
                : List.of();
        List<JobPostResponse> jobPosts = member.getRole() == MemberRole.GRADUATE
                ? jobPostService.getMyJobPosts(member)
                : List.of();

        return new MyPageResponse(profile, activity, techStacks, targetJobs, jobPosts);
    }
}

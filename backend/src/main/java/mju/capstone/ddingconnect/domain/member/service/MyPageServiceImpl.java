package mju.capstone.ddingconnect.domain.member.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.coffeechat.service.CoffeeChatService;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.ReplaceTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
import mju.capstone.ddingconnect.domain.interested_job.service.TargetJobService;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostLinkRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.response.JobPostResponse;
import mju.capstone.ddingconnect.domain.job_post.service.JobPostService;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateGraduateMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateStudentMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.MyPageResponse;
import mju.capstone.ddingconnect.domain.qna.question.service.QuestionService;
import mju.capstone.ddingconnect.domain.roadmap.service.RoadmapService;
import mju.capstone.ddingconnect.domain.techstack.dto.request.ReplaceTechStackRequest;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;
import mju.capstone.ddingconnect.domain.techstack.service.TechStackService;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.MemberHandler;
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
     * 재학생 마이페이지 통합 수정.
     * 진입 가드 — STUDENT 가 아니면 MEMBER_FIELD_ROLE_MISMATCH(UNKNOWN 도 거부).
     * 단일 @Transactional 안에서 위임 도메인 메서드(REQUIRED 전파)가 모두 참여 → 부분 저장 없음.
     * 각 항목은 null 이면 변경하지 않는다(부분 수정).
     */
    @Override
    @Transactional
    public MyPageResponse updateStudentMyPage(Member member, UpdateStudentMyPageRequest request) {
        if (member.getRole() != MemberRole.STUDENT) {
            throw new MemberHandler(ErrorStatus.MEMBER_FIELD_ROLE_MISMATCH);
        }

        MemberResponse profile = request.profile() != null
                ? memberService.updateMyProfile(member, request.profile().toUpdateMemberRequest())
                : memberService.getMyProfile(member);

        if (request.techStacks() != null) {
            techStackService.replace(member, new ReplaceTechStackRequest(request.techStacks()));
        }

        if (request.targetJobs() != null) {
            targetJobService.replace(member, new ReplaceTargetJobRequest(request.targetJobs()));
        }

        return buildResponse(member, profile);
    }

    /**
     * 졸업생 마이페이지 통합 수정.
     * 진입 가드 — GRADUATE 가 아니면 MEMBER_FIELD_ROLE_MISMATCH(UNKNOWN 도 거부).
     * 단일 @Transactional 안에서 위임 도메인 메서드(REQUIRED 전파)가 모두 참여 → 부분 저장 없음.
     * jobPostIdsToDelete 를 먼저, jobPostsToAdd 를 나중에 처리.
     */
    @Override
    @Transactional
    public MyPageResponse updateGraduateMyPage(Member member, UpdateGraduateMyPageRequest request) {
        if (member.getRole() != MemberRole.GRADUATE) {
            throw new MemberHandler(ErrorStatus.MEMBER_FIELD_ROLE_MISMATCH);
        }

        MemberResponse profile = request.profile() != null
                ? memberService.updateMyProfile(member, request.profile().toUpdateMemberRequest())
                : memberService.getMyProfile(member);

        if (request.techStacks() != null) {
            techStackService.replace(member, new ReplaceTechStackRequest(request.techStacks()));
        }

        if (request.jobPostIdsToDelete() != null) {
            for (Long jobPostId : request.jobPostIdsToDelete()) {
                jobPostService.delete(member, jobPostId);
            }
        }
        if (request.jobPostsToAdd() != null) {
            for (CreateJobPostLinkRequest toAdd : request.jobPostsToAdd()) {
                jobPostService.createFromLink(member, toAdd);
            }
        }

        return buildResponse(member, profile);
    }

    /**
     * profile 을 제외한 마이페이지 구성 요소(활동 통계/기술 스택/역할별 항목)를 조회해 한 응답으로 조합한다.
     * getMyPage(조회)와 updateStudent/GraduateMyPage(수정 후 최신 조회)가 공유한다.
     *
     * 역할별 비해당 필드는 null 로 두고 @JsonInclude(NON_NULL) 가 응답 JSON 에서 키 자체를 제외한다.
     * 빈 배열 []는 "역할 맞지만 0개" 의미로 보존된다.
     * point 는 마이페이지에선 항상 제외(withoutPoint).
     */
    private MyPageResponse buildResponse(Member member, MemberResponse profile) {
        // 마이페이지에선 point 를 노출하지 않는다(다른 엔드포인트는 영향 없음)
        MemberResponse profileForMyPage = profile.withoutPoint();

        // 로드맵은 STUDENT 전용 항목 — GRADUATE/UNKNOWN 은 null → 응답에서 키 자체 제외
        Long roadmapCount = member.getRole() == MemberRole.STUDENT
                ? roadmapService.countMyRoadmaps(member)
                : null;

        MyPageResponse.ActivityStats activity = new MyPageResponse.ActivityStats(
                coffeeChatService.countMyAcceptedCoffeeChats(member),
                roadmapCount,
                questionService.countMyQuestions(member)
        );

        List<TechStackResponse> techStacks = techStackService.getMyTechStacks(member);

        // 관심 직군은 STUDENT 전용, 등록 구직 공고는 GRADUATE 전용 — 비해당 역할은 null
        List<TargetJobResponse> targetJobs = member.getRole() == MemberRole.STUDENT
                ? targetJobService.getMyTargetJobs(member)
                : null;
        List<JobPostResponse> jobPosts = member.getRole() == MemberRole.GRADUATE
                ? jobPostService.getMyJobPosts(member)
                : null;

        return new MyPageResponse(profileForMyPage, activity, techStacks, targetJobs, jobPosts);
    }
}

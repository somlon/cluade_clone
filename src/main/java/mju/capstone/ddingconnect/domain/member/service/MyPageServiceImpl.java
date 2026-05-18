package mju.capstone.ddingconnect.domain.member.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.coffeechat.service.CoffeeChatService;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
import mju.capstone.ddingconnect.domain.interested_job.service.TargetJobService;
import mju.capstone.ddingconnect.domain.job_post.dto.response.JobPostResponse;
import mju.capstone.ddingconnect.domain.job_post.service.JobPostService;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.MyPageResponse;
import mju.capstone.ddingconnect.domain.qna.question.service.QuestionService;
import mju.capstone.ddingconnect.domain.roadmap.service.RoadmapService;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;
import mju.capstone.ddingconnect.domain.techstack.service.TechStackService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * [마이페이지 서비스 구현체]
 * 항목별 도메인 서비스의 조회 결과를 in-process 로 조합하는 애그리게이터.
 * 자체 레포지토리 의존 없이 각 도메인 서비스만 호출한다.
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
        MemberResponse profile = memberService.getMyProfile(member);

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

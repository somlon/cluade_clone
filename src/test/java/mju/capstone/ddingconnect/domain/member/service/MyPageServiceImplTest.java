package mju.capstone.ddingconnect.domain.member.service;

import mju.capstone.ddingconnect.domain.coffeechat.service.CoffeeChatService;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
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
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;
import mju.capstone.ddingconnect.domain.techstack.service.TechStackService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MyPageServiceImpl 단위 테스트")
class MyPageServiceImplTest {

    @Mock MemberService memberService;
    @Mock TechStackService techStackService;
    @Mock TargetJobService targetJobService;
    @Mock CoffeeChatService coffeeChatService;
    @Mock RoadmapService roadmapService;
    @Mock QuestionService questionService;
    @Mock JobPostService jobPostService;

    @InjectMocks MyPageServiceImpl myPageService;

    private Member studentMember() {
        return Member.builder().id(1L).email("s@mju.ac.kr").nickname("재학생")
                .role(MemberRole.STUDENT).build();
    }

    private Member graduateMember() {
        return Member.builder().id(2L).email("g@mju.ac.kr").nickname("졸업생")
                .role(MemberRole.GRADUATE).build();
    }

    private MemberResponse profileOf(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getNickname(),
                null, null, null, null, null, null, 0L, member.getRole(),
                null, null, null, null);
    }

    @Test
    @DisplayName("getMyPage - 재학생: 프로필·활동 통계·기술 스택·관심 직군을 조합하고 jobPosts 는 비운다")
    void getMyPage_재학생() {
        Member member = studentMember();
        when(memberService.getMyProfile(member)).thenReturn(profileOf(member));
        when(coffeeChatService.countMyAcceptedCoffeeChats(member)).thenReturn(2L);
        when(roadmapService.countMyRoadmaps(member)).thenReturn(1L);
        when(questionService.countMyQuestions(member)).thenReturn(5L);
        when(techStackService.getMyTechStacks(member))
                .thenReturn(List.of(new TechStackResponse(10L, TechStackName.JAVA)));
        when(targetJobService.getMyTargetJobs(member))
                .thenReturn(List.of(new TargetJobResponse(20L, TargetJobCategory.BACKEND, null)));

        MyPageResponse response = myPageService.getMyPage(member);

        assertThat(response.profile().role()).isEqualTo(MemberRole.STUDENT);
        assertThat(response.activity().coffeeChatCount()).isEqualTo(2L);
        assertThat(response.activity().roadmapCount()).isEqualTo(1L);
        assertThat(response.activity().questionCount()).isEqualTo(5L);
        assertThat(response.techStacks()).hasSize(1);
        assertThat(response.targetJobs()).hasSize(1);
        assertThat(response.jobPosts()).isEmpty();

        // 재학생이면 졸업생 항목 서비스는 호출하지 않는다
        verify(jobPostService, never()).getMyJobPosts(member);
    }

    @Test
    @DisplayName("getMyPage - 졸업생: 등록 구직 공고를 조합하고 targetJobs 는 비운다")
    void getMyPage_졸업생() {
        Member member = graduateMember();
        when(memberService.getMyProfile(member)).thenReturn(profileOf(member));
        when(coffeeChatService.countMyAcceptedCoffeeChats(member)).thenReturn(0L);
        when(roadmapService.countMyRoadmaps(member)).thenReturn(0L);
        when(questionService.countMyQuestions(member)).thenReturn(0L);
        when(techStackService.getMyTechStacks(member)).thenReturn(List.of());
        when(jobPostService.getMyJobPosts(member)).thenReturn(List.of(
                new JobPostResponse(100L, "네이버", null, null, null, null, null, null, null, List.of())));

        MyPageResponse response = myPageService.getMyPage(member);

        assertThat(response.profile().role()).isEqualTo(MemberRole.GRADUATE);
        assertThat(response.jobPosts()).hasSize(1);
        assertThat(response.targetJobs()).isEmpty();

        // 졸업생이면 관심 직군 서비스는 호출하지 않는다
        verify(targetJobService, never()).getMyTargetJobs(member);
    }
}

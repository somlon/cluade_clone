package mju.capstone.ddingconnect.domain.member.service;

import mju.capstone.ddingconnect.domain.coffeechat.service.CoffeeChatService;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.ReplaceTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
import mju.capstone.ddingconnect.domain.interested_job.service.TargetJobService;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.response.JobPostResponse;
import mju.capstone.ddingconnect.domain.job_post.service.JobPostService;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMemberRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.MyPageResponse;
import mju.capstone.ddingconnect.domain.qna.question.service.QuestionService;
import mju.capstone.ddingconnect.domain.roadmap.service.RoadmapService;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import mju.capstone.ddingconnect.domain.techstack.dto.request.ReplaceTechStackRequest;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;
import mju.capstone.ddingconnect.domain.techstack.service.TechStackService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MyPageServiceImpl 단위 테스트")
class MyPageServiceImplTest {

    /** 위임받은 도메인 수정 메서드가 던지는 실패를 대표하는 예외 메시지. */
    private static final String DELEGATE_FAILURE_MESSAGE = "위임 도메인 수정 실패";

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
        return new MemberResponse(member.getId(), member.getEmail(), null, member.getNickname(),
                null, null, null, null, null, null, 0L, member.getRole(),
                null, null, null, null, null);
    }

    /** 닉네임만 바꾸는 프로필 수정 요청 (나머지 필드는 미전송). */
    private UpdateMemberRequest profileUpdateRequest() {
        return new UpdateMemberRequest(null, null, "변경된닉네임", null, null,
                null, null, null, null, null, null, null, null, null);
    }

    /** 회사명만 채운 구직 공고 등록 요청. */
    private CreateJobPostRequest jobPostRequest(String companyName) {
        return new CreateJobPostRequest(null, null, null, null, null,
                null, null, null, null, null, companyName);
    }

    @Test
    @DisplayName("getMyPage - 재학생: 프로필·활동 통계·기술 스택·관심 직군을 조합하고 jobPosts 는 비운다")
    void getMyPageForStudent() {
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
    void getMyPageForGraduate() {
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

    // ── 통합 수정 (updateMyPage) ──────────────────────────────────────

    @Test
    @DisplayName("updateMyPage - 재학생: 프로필·기술 스택·관심 직군 수정을 각 도메인 서비스에 위임한다")
    void updateMyPageDelegatesAllItemsForStudent() {
        Member member = studentMember();
        UpdateMemberRequest profileReq = profileUpdateRequest();
        List<TechStackName> techStacks = List.of(TechStackName.JAVA);
        List<TargetJobCategory> targetJobs = List.of(TargetJobCategory.BACKEND);
        UpdateMyPageRequest request =
                new UpdateMyPageRequest(profileReq, techStacks, targetJobs, null, null);

        MemberResponse updatedProfile = profileOf(member);
        when(memberService.updateMyProfile(member, profileReq)).thenReturn(updatedProfile);

        MyPageResponse response = myPageService.updateMyPage(member, request);

        // 프로필 응답은 수정 API(updateMyProfile)가 반환한 값을 그대로 싣는다
        assertThat(response.profile()).isSameAs(updatedProfile);
        verify(techStackService).replace(member, new ReplaceTechStackRequest(techStacks));
        verify(targetJobService).replace(member, new ReplaceTargetJobRequest(targetJobs));
        // profile 이 non-null 이면 조회(getMyProfile)가 아닌 수정(updateMyProfile)을 탄다
        verify(memberService, never()).getMyProfile(member);
        // 재학생 요청엔 졸업생 공고 항목이 없다
        verify(jobPostService, never()).delete(any(), any());
        verify(jobPostService, never()).create(any(), any());
    }

    @Test
    @DisplayName("updateMyPage - 졸업생: 구직 공고를 '삭제 먼저, 추가 나중' 순서로 위임한다")
    void updateMyPageDelegatesJobPostAddAndRemoveForGraduate() {
        Member member = graduateMember();
        List<Long> idsToDelete = List.of(100L, 200L);
        List<CreateJobPostRequest> toAdd = List.of(jobPostRequest("네이버"), jobPostRequest("카카오"));
        // profile·techStacks·targetJobs 는 미전송(null) — 졸업생 공고만 변경
        UpdateMyPageRequest request =
                new UpdateMyPageRequest(null, null, null, toAdd, idsToDelete);

        when(memberService.getMyProfile(member)).thenReturn(profileOf(member));

        myPageService.updateMyPage(member, request);

        // 삭제(jobPostIdsToDelete) → 추가(jobPostsToAdd) 순서로 위임된다
        InOrder inOrder = inOrder(jobPostService);
        inOrder.verify(jobPostService).delete(member, 100L);
        inOrder.verify(jobPostService).delete(member, 200L);
        inOrder.verify(jobPostService).create(member, toAdd.get(0));
        inOrder.verify(jobPostService).create(member, toAdd.get(1));
        // profile 미전송 → 수정이 아닌 조회로 최신 프로필을 채운다
        verify(memberService).getMyProfile(member);
        verify(memberService, never()).updateMyProfile(any(), any());
    }

    @Test
    @DisplayName("updateMyPage - 모든 항목 미전송(null): 어떤 도메인 수정도 위임하지 않는다")
    void updateMyPageSkipsUnsentItems() {
        Member member = studentMember();
        UpdateMyPageRequest request = new UpdateMyPageRequest(null, null, null, null, null);

        when(memberService.getMyProfile(member)).thenReturn(profileOf(member));

        myPageService.updateMyPage(member, request);

        verify(memberService, never()).updateMyProfile(any(), any());
        verify(techStackService, never()).replace(any(), any());
        verify(targetJobService, never()).replace(any(), any());
        verify(jobPostService, never()).delete(any(), any());
        verify(jobPostService, never()).create(any(), any());
    }

    @Test
    @DisplayName("updateMyPage - 위임 중 하나가 실패하면 예외를 전파하고 이후 위임을 실행하지 않는다(전체 롤백)")
    void updateMyPagePropagatesExceptionOnDelegationFailure() {
        Member member = studentMember();
        List<TechStackName> techStacks = List.of(TechStackName.JAVA);
        List<TargetJobCategory> targetJobs = List.of(TargetJobCategory.BACKEND);
        List<CreateJobPostRequest> toAdd = List.of(jobPostRequest("네이버"));
        UpdateMyPageRequest request = new UpdateMyPageRequest(
                profileUpdateRequest(), techStacks, targetJobs, toAdd, List.of(100L));

        // 위임 파이프라인 중간(관심 직군 교체)에서 실패가 발생
        when(targetJobService.replace(any(), any()))
                .thenThrow(new RuntimeException(DELEGATE_FAILURE_MESSAGE));

        assertThatThrownBy(() -> myPageService.updateMyPage(member, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(DELEGATE_FAILURE_MESSAGE);

        // 실패 지점 이후 위임(졸업생 공고 삭제·추가)은 실행되지 않는다.
        // updateMyPage 는 단일 @Transactional 이므로 전파된 예외로 트랜잭션 전체가 롤백된다.
        verify(jobPostService, never()).delete(any(), any());
        verify(jobPostService, never()).create(any(), any());
    }
}

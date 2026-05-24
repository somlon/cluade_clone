package mju.capstone.ddingconnect.domain.member.service;

import mju.capstone.ddingconnect.domain.coffeechat.service.CoffeeChatService;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.ReplaceTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
import mju.capstone.ddingconnect.domain.interested_job.service.TargetJobService;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostLinkRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.response.JobPostResponse;
import mju.capstone.ddingconnect.domain.job_post.service.JobPostService;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateGraduateMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateGraduateProfileRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateStudentMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateStudentProfileRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.MyPageResponse;
import mju.capstone.ddingconnect.domain.qna.question.service.QuestionService;
import mju.capstone.ddingconnect.domain.roadmap.service.RoadmapService;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import mju.capstone.ddingconnect.domain.techstack.dto.request.ReplaceTechStackRequest;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;
import mju.capstone.ddingconnect.domain.techstack.service.TechStackService;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.MemberHandler;
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
import static org.mockito.Mockito.verifyNoInteractions;
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

    private Member unknownMember() {
        return Member.builder().id(3L).email("u@mju.ac.kr").nickname("미정")
                .role(MemberRole.UNKNOWN).build();
    }

    private MemberResponse profileOf(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), null, member.getNickname(),
                null, null, null, null, null, null, 0L, member.getRole(),
                null, null, null, null, null);
    }

    /** 닉네임만 바꾸는 STUDENT 프로필 수정 요청 (나머지 필드는 미전송). */
    private UpdateStudentProfileRequest studentProfileUpdate() {
        return new UpdateStudentProfileRequest(null, null, "변경된닉네임", null, null,
                null, null, null, null, null);
    }

    /** 닉네임만 바꾸는 GRADUATE 프로필 수정 요청 (나머지 필드는 미전송). */
    private UpdateGraduateProfileRequest graduateProfileUpdate() {
        return new UpdateGraduateProfileRequest(null, null, "변경된닉네임", null, null,
                null, null, null, null, null, null, null, null);
    }

    private CreateJobPostLinkRequest linkRequest(String url) {
        return new CreateJobPostLinkRequest(url);
    }

    // ── 조회 (getMyPage) ──────────────────────────────────────────────

    @Test
    @DisplayName("getMyPage - 재학생: 프로필·활동 통계·기술 스택·관심 직군을 조합하고 jobPosts 는 null(응답 키 제외)")
    void getMyPageForStudent() {
        Member member = studentMember();
        when(memberService.getMyProfile(member)).thenReturn(profileOf(member));
        when(coffeeChatService.countMyAcceptedCoffeeChats(member)).thenReturn(2L);
        when(roadmapService.countMyRoadmaps(member)).thenReturn(1L);
        when(questionService.countMyQuestions(member)).thenReturn(5L);
        when(techStackService.getMyTechStacks(member))
                .thenReturn(List.of(new TechStackResponse(10L, TechStackName.JAVA)));
        when(targetJobService.getMyTargetJobs(member))
                .thenReturn(List.of(new TargetJobResponse(20L, TargetJobCategory.BACKEND)));

        MyPageResponse response = myPageService.getMyPage(member);

        assertThat(response.profile().role()).isEqualTo(MemberRole.STUDENT);
        assertThat(response.profile().point()).isNull();  // 마이페이지엔 point 항상 제외
        assertThat(response.activity().coffeeChatCount()).isEqualTo(2L);
        assertThat(response.activity().roadmapCount()).isEqualTo(1L);
        assertThat(response.activity().questionCount()).isEqualTo(5L);
        assertThat(response.techStacks()).hasSize(1);
        assertThat(response.targetJobs()).hasSize(1);
        assertThat(response.jobPosts()).isNull();  // STUDENT 는 jobPosts 비해당 → null

        // 재학생이면 졸업생 항목 서비스는 호출하지 않는다
        verify(jobPostService, never()).getMyJobPosts(member);
    }

    @Test
    @DisplayName("getMyPage - 졸업생: 등록 구직 공고를 조합하고 targetJobs/roadmapCount 는 null(응답 키 제외)")
    void getMyPageForGraduate() {
        Member member = graduateMember();
        when(memberService.getMyProfile(member)).thenReturn(profileOf(member));
        when(coffeeChatService.countMyAcceptedCoffeeChats(member)).thenReturn(0L);
        when(questionService.countMyQuestions(member)).thenReturn(0L);
        when(techStackService.getMyTechStacks(member)).thenReturn(List.of());
        when(jobPostService.getMyJobPosts(member)).thenReturn(List.of(
                new JobPostResponse(100L, "네이버", null, null, null, null, null, null, null, List.of())));

        MyPageResponse response = myPageService.getMyPage(member);

        assertThat(response.profile().role()).isEqualTo(MemberRole.GRADUATE);
        assertThat(response.profile().point()).isNull();        // 마이페이지엔 point 항상 제외
        assertThat(response.activity().roadmapCount()).isNull(); // GRADUATE 는 roadmapCount 비해당 → null
        assertThat(response.jobPosts()).hasSize(1);
        assertThat(response.targetJobs()).isNull();              // GRADUATE 는 targetJobs 비해당 → null

        // 졸업생이면 관심 직군·로드맵 카운트 서비스는 호출하지 않는다
        verify(targetJobService, never()).getMyTargetJobs(member);
        verify(roadmapService, never()).countMyRoadmaps(member);
    }

    @Test
    @DisplayName("getMyPage - UNKNOWN: 로드맵 카운트는 null 고정, 양쪽 역할 리스트 모두 null")
    void getMyPageForUnknownSkipsRoadmapCount() {
        Member member = unknownMember();
        when(memberService.getMyProfile(member)).thenReturn(profileOf(member));
        when(coffeeChatService.countMyAcceptedCoffeeChats(member)).thenReturn(0L);
        when(questionService.countMyQuestions(member)).thenReturn(0L);
        when(techStackService.getMyTechStacks(member)).thenReturn(List.of());

        MyPageResponse response = myPageService.getMyPage(member);

        assertThat(response.activity().roadmapCount()).isNull();
        assertThat(response.targetJobs()).isNull();
        assertThat(response.jobPosts()).isNull();
        verify(roadmapService, never()).countMyRoadmaps(member);
        verify(targetJobService, never()).getMyTargetJobs(member);
        verify(jobPostService, never()).getMyJobPosts(member);
    }

    @Test
    @DisplayName("getMyPage - STUDENT 의 targetJobs 가 0개일 때 빈 리스트로 유지 (null 아님 — '해당 역할 + 0개' 의미)")
    void getMyPageForStudentPreservesEmptyTargetJobs() {
        Member member = studentMember();
        when(memberService.getMyProfile(member)).thenReturn(profileOf(member));
        when(coffeeChatService.countMyAcceptedCoffeeChats(member)).thenReturn(0L);
        when(roadmapService.countMyRoadmaps(member)).thenReturn(0L);
        when(questionService.countMyQuestions(member)).thenReturn(0L);
        when(techStackService.getMyTechStacks(member)).thenReturn(List.of());
        when(targetJobService.getMyTargetJobs(member)).thenReturn(List.of());

        MyPageResponse response = myPageService.getMyPage(member);

        assertThat(response.targetJobs()).isNotNull().isEmpty();
        assertThat(response.jobPosts()).isNull();
    }

    @Test
    @DisplayName("getMyPage - GRADUATE 의 jobPosts 가 0개일 때 빈 리스트로 유지 (null 아님)")
    void getMyPageForGraduatePreservesEmptyJobPosts() {
        Member member = graduateMember();
        when(memberService.getMyProfile(member)).thenReturn(profileOf(member));
        when(coffeeChatService.countMyAcceptedCoffeeChats(member)).thenReturn(0L);
        when(questionService.countMyQuestions(member)).thenReturn(0L);
        when(techStackService.getMyTechStacks(member)).thenReturn(List.of());
        when(jobPostService.getMyJobPosts(member)).thenReturn(List.of());

        MyPageResponse response = myPageService.getMyPage(member);

        assertThat(response.jobPosts()).isNotNull().isEmpty();
        assertThat(response.targetJobs()).isNull();
    }

    // ── 재학생 통합 수정 (updateStudentMyPage) ───────────────────────

    @Test
    @DisplayName("updateStudentMyPage - 프로필·기술 스택·관심 직군 수정을 각 도메인 서비스에 위임한다")
    void updateStudentMyPageDelegatesAllItems() {
        Member member = studentMember();
        UpdateStudentProfileRequest profileReq = studentProfileUpdate();
        List<TechStackName> techStacks = List.of(TechStackName.JAVA);
        List<TargetJobCategory> targetJobs = List.of(TargetJobCategory.BACKEND);
        UpdateStudentMyPageRequest request =
                new UpdateStudentMyPageRequest(profileReq, techStacks, targetJobs);

        MemberResponse updatedProfile = profileOf(member);
        when(memberService.updateMyProfile(any(), any())).thenReturn(updatedProfile);

        MyPageResponse response = myPageService.updateStudentMyPage(member, request);

        // 프로필 응답은 수정 API(updateMyProfile) 반환값을 point 만 제외해서 싣는다 (마이페이지 정책)
        assertThat(response.profile()).isEqualTo(updatedProfile.withoutPoint());
        assertThat(response.profile().point()).isNull();
        // 어댑터 변환된 UpdateMemberRequest 로 위임된다 — 시그니처만 확인
        verify(memberService).updateMyProfile(any(), any());
        verify(techStackService).replace(member, new ReplaceTechStackRequest(techStacks));
        verify(targetJobService).replace(member, new ReplaceTargetJobRequest(targetJobs));
        // profile 이 non-null 이면 조회(getMyProfile)가 아닌 수정(updateMyProfile)을 탄다
        verify(memberService, never()).getMyProfile(member);
        // 재학생 경로엔 졸업생 공고 분기 자체가 없다
        verifyNoInteractions(jobPostService);
    }

    @Test
    @DisplayName("updateStudentMyPage - 모든 항목 미전송(null): 어떤 도메인 수정도 위임하지 않는다")
    void updateStudentMyPageSkipsUnsentItems() {
        Member member = studentMember();
        UpdateStudentMyPageRequest request = new UpdateStudentMyPageRequest(null, null, null);

        when(memberService.getMyProfile(member)).thenReturn(profileOf(member));

        myPageService.updateStudentMyPage(member, request);

        verify(memberService, never()).updateMyProfile(any(), any());
        verify(techStackService, never()).replace(any(), any());
        verify(targetJobService, never()).replace(any(), any());
        verifyNoInteractions(jobPostService);
    }

    @Test
    @DisplayName("updateStudentMyPage - GRADUATE 회원이 호출하면 MEMBER_FIELD_ROLE_MISMATCH 로 거부한다")
    void updateStudentMyPageRejectsGraduate() {
        Member member = graduateMember();
        UpdateStudentMyPageRequest request = new UpdateStudentMyPageRequest(null, null, null);

        assertThatThrownBy(() -> myPageService.updateStudentMyPage(member, request))
                .isInstanceOf(MemberHandler.class)
                .matches(e -> ((MemberHandler) e).getErrorReasonHttpStatus().getCode()
                        .equals(ErrorStatus.MEMBER_FIELD_ROLE_MISMATCH.getCode()));

        verifyNoInteractions(memberService, techStackService, targetJobService, jobPostService);
    }

    @Test
    @DisplayName("updateStudentMyPage - UNKNOWN 회원이 호출하면 MEMBER_FIELD_ROLE_MISMATCH 로 거부한다")
    void updateStudentMyPageRejectsUnknown() {
        Member member = unknownMember();
        UpdateStudentMyPageRequest request = new UpdateStudentMyPageRequest(null, null, null);

        assertThatThrownBy(() -> myPageService.updateStudentMyPage(member, request))
                .isInstanceOf(MemberHandler.class);

        verifyNoInteractions(memberService, techStackService, targetJobService, jobPostService);
    }

    @Test
    @DisplayName("updateStudentMyPage - 위임 중 하나가 실패하면 예외를 전파한다(전체 롤백)")
    void updateStudentMyPagePropagatesExceptionOnDelegationFailure() {
        Member member = studentMember();
        List<TechStackName> techStacks = List.of(TechStackName.JAVA);
        List<TargetJobCategory> targetJobs = List.of(TargetJobCategory.BACKEND);
        UpdateStudentMyPageRequest request = new UpdateStudentMyPageRequest(
                studentProfileUpdate(), techStacks, targetJobs);

        when(memberService.updateMyProfile(any(), any())).thenReturn(profileOf(member));
        // 위임 파이프라인 중간(관심 직군 교체)에서 실패가 발생
        when(targetJobService.replace(any(), any()))
                .thenThrow(new RuntimeException(DELEGATE_FAILURE_MESSAGE));

        assertThatThrownBy(() -> myPageService.updateStudentMyPage(member, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(DELEGATE_FAILURE_MESSAGE);
    }

    // ── 졸업생 통합 수정 (updateGraduateMyPage) ──────────────────────

    @Test
    @DisplayName("updateGraduateMyPage - 구직 공고를 '삭제 먼저, 추가 나중' 순서로 위임한다")
    void updateGraduateMyPageDelegatesJobPostAddAndRemove() {
        Member member = graduateMember();
        List<Long> idsToDelete = List.of(100L, 200L);
        List<CreateJobPostLinkRequest> toAdd = List.of(
                linkRequest("https://jobs.example.com/1"),
                linkRequest("https://jobs.example.com/2"));
        // profile·techStacks 는 미전송(null) — 졸업생 공고만 변경
        UpdateGraduateMyPageRequest request =
                new UpdateGraduateMyPageRequest(null, null, toAdd, idsToDelete);

        when(memberService.getMyProfile(member)).thenReturn(profileOf(member));

        myPageService.updateGraduateMyPage(member, request);

        // 삭제(jobPostIdsToDelete) → 추가(jobPostsToAdd) 순서로 위임된다
        InOrder inOrder = inOrder(jobPostService);
        inOrder.verify(jobPostService).delete(member, 100L);
        inOrder.verify(jobPostService).delete(member, 200L);
        inOrder.verify(jobPostService).createFromLink(member, toAdd.get(0));
        inOrder.verify(jobPostService).createFromLink(member, toAdd.get(1));
        // profile 미전송 → 수정이 아닌 조회로 최신 프로필을 채운다
        verify(memberService).getMyProfile(member);
        verify(memberService, never()).updateMyProfile(any(), any());
        // 졸업생 경로엔 관심 직군 분기 자체가 없다
        verifyNoInteractions(targetJobService);
    }

    @Test
    @DisplayName("updateGraduateMyPage - 프로필·기술 스택·공고 추가를 함께 위임한다")
    void updateGraduateMyPageDelegatesFullRequest() {
        Member member = graduateMember();
        UpdateGraduateProfileRequest profileReq = graduateProfileUpdate();
        List<TechStackName> techStacks = List.of(TechStackName.SPRING);
        List<CreateJobPostLinkRequest> toAdd = List.of(linkRequest("https://jobs.example.com/x"));
        UpdateGraduateMyPageRequest request =
                new UpdateGraduateMyPageRequest(profileReq, techStacks, toAdd, null);

        MemberResponse updatedProfile = profileOf(member);
        when(memberService.updateMyProfile(any(), any())).thenReturn(updatedProfile);

        MyPageResponse response = myPageService.updateGraduateMyPage(member, request);

        // 프로필 응답은 수정 API 반환값을 point 만 제외해서 싣는다 (마이페이지 정책)
        assertThat(response.profile()).isEqualTo(updatedProfile.withoutPoint());
        assertThat(response.profile().point()).isNull();
        verify(memberService).updateMyProfile(any(), any());
        verify(techStackService).replace(member, new ReplaceTechStackRequest(techStacks));
        verify(jobPostService).createFromLink(member, toAdd.get(0));
        verify(memberService, never()).getMyProfile(member);
        verifyNoInteractions(targetJobService);
    }

    @Test
    @DisplayName("updateGraduateMyPage - STUDENT 회원이 호출하면 MEMBER_FIELD_ROLE_MISMATCH 로 거부한다")
    void updateGraduateMyPageRejectsStudent() {
        Member member = studentMember();
        UpdateGraduateMyPageRequest request = new UpdateGraduateMyPageRequest(null, null, null, null);

        assertThatThrownBy(() -> myPageService.updateGraduateMyPage(member, request))
                .isInstanceOf(MemberHandler.class);

        verifyNoInteractions(memberService, techStackService, targetJobService, jobPostService);
    }

    @Test
    @DisplayName("updateGraduateMyPage - UNKNOWN 회원이 호출하면 MEMBER_FIELD_ROLE_MISMATCH 로 거부한다")
    void updateGraduateMyPageRejectsUnknown() {
        Member member = unknownMember();
        UpdateGraduateMyPageRequest request = new UpdateGraduateMyPageRequest(null, null, null, null);

        assertThatThrownBy(() -> myPageService.updateGraduateMyPage(member, request))
                .isInstanceOf(MemberHandler.class);

        verifyNoInteractions(memberService, techStackService, targetJobService, jobPostService);
    }

    @Test
    @DisplayName("updateGraduateMyPage - 모든 항목 미전송(null): 어떤 도메인 수정도 위임하지 않는다")
    void updateGraduateMyPageSkipsUnsentItems() {
        Member member = graduateMember();
        UpdateGraduateMyPageRequest request = new UpdateGraduateMyPageRequest(null, null, null, null);

        when(memberService.getMyProfile(member)).thenReturn(profileOf(member));

        myPageService.updateGraduateMyPage(member, request);

        verify(memberService, never()).updateMyProfile(any(), any());
        verify(techStackService, never()).replace(any(), any());
        verifyNoInteractions(targetJobService);
        verify(jobPostService, never()).delete(any(), any());
        verify(jobPostService, never()).createFromLink(any(), any());
    }
}

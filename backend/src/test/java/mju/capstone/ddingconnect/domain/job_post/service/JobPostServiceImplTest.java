package mju.capstone.ddingconnect.domain.job_post.service;

import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJob;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.interested_job.domain.repository.TargetJobRepository;
import mju.capstone.ddingconnect.domain.job_post.domain.*;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.GraduateJobPostRepository;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.JobAlarmRepository;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.PostContentsRepository;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.request.UpdateJobPostRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.response.JobPostResponse;
import mju.capstone.ddingconnect.domain.member.domain.Graduate;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.domain.repository.GraduateRepository;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.JobPostHandler;
import mju.capstone.ddingconnect.global.sse.AlarmNotificationEvent;
import mju.capstone.ddingconnect.global.alarm.AlarmType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobPostServiceImpl 단위 테스트")
class JobPostServiceImplTest {

    @Mock PostContentsRepository postContentsRepository;
    @Mock GraduateJobPostRepository graduateJobPostRepository;
    @Mock JobAlarmRepository jobAlarmRepository;
    @Mock GraduateRepository graduateRepository;
    @Mock TargetJobRepository targetJobRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks JobPostServiceImpl jobPostService;

    private Member graduateMember;
    private Member otherMember;
    private Graduate graduate;
    private PostContents postContents;

    @BeforeEach
    void setUp() {
        graduateMember = Member.builder().id(1L).email("g@mju.ac.kr").nickname("졸업생")
                .role(MemberRole.GRADUATE).build();
        otherMember = Member.builder().id(2L).email("o@mju.ac.kr").nickname("타인")
                .role(MemberRole.GRADUATE).build();
        graduate = Graduate.builder().id(10L).member(graduateMember)
                .company("네이버").careerYear(5).build();
        postContents = PostContents.builder().id(100L)
                .companyName("네이버").region("성남")
                .careerType(CareerType.NEW_GRADUATE).jobType(JobType.BACKEND)
                .deadline(LocalDate.of(2026, 6, 30))
                .preferredLanguages(List.of("Java")).build();
    }

    private CreateJobPostRequest createReq() {
        return new CreateJobPostRequest("img", "성남", CareerType.NEW_GRADUATE, JobType.BACKEND,
                "한국", "성남시", "분당구", LocalDate.of(2026, 6, 30),
                "https://test.com", List.of("JavaScript", "React", "Node.js"), "네이버");
    }

    @Test
    @DisplayName("create - 졸업생이 구직 공고를 정상 등록한다 (매칭되는 TargetJob 없으면 알람 미발행)")
    void createByGraduateSucceeds() {
        when(postContentsRepository.save(any(PostContents.class))).thenReturn(postContents);
        when(graduateRepository.findByMemberId(graduateMember.getId())).thenReturn(Optional.of(graduate));
        when(targetJobRepository.findByInterestedJob(TargetJobCategory.BACKEND)).thenReturn(List.of());

        JobPostResponse response = jobPostService.create(graduateMember, createReq());

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.companyName()).isEqualTo("네이버");
        verify(postContentsRepository).save(any(PostContents.class));
        verify(graduateJobPostRepository).save(any(GraduateJobPost.class));
        verify(jobAlarmRepository, never()).save(any(JobAlarm.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("create - 매칭되는 학생 N명에게 JobAlarm 발행, 본인/중복 멤버는 제외")
    void createPublishesAlarmToMatchingStudents() {
        Member studentA = Member.builder().id(11L).nickname("A").build();
        Member studentB = Member.builder().id(12L).nickname("B").build();
        // 본인 (등록한 졸업생) 도 같은 카테고리 관심을 가진 경우 → 제외돼야 함
        TargetJob selfMatch = TargetJob.builder().id(1L).member(graduateMember)
                .interestedJob(TargetJobCategory.BACKEND).build();
        TargetJob aMatch = TargetJob.builder().id(2L).member(studentA)
                .interestedJob(TargetJobCategory.BACKEND).build();
        // 같은 멤버가 동일 카테고리 중복 등록 → 1건만 발행
        TargetJob aDuplicate = TargetJob.builder().id(3L).member(studentA)
                .interestedJob(TargetJobCategory.BACKEND).build();
        TargetJob bMatch = TargetJob.builder().id(4L).member(studentB)
                .interestedJob(TargetJobCategory.BACKEND).build();

        when(postContentsRepository.save(any(PostContents.class))).thenReturn(postContents);
        when(graduateRepository.findByMemberId(graduateMember.getId())).thenReturn(Optional.of(graduate));
        when(targetJobRepository.findByInterestedJob(TargetJobCategory.BACKEND))
                .thenReturn(List.of(selfMatch, aMatch, aDuplicate, bMatch));

        jobPostService.create(graduateMember, createReq());

        // A, B 각각 1건씩 = 총 2건. self 와 duplicate 는 제외
        verify(jobAlarmRepository, times(2)).save(any(JobAlarm.class));

        // SSE 푸시 이벤트도 동일하게 A, B 대상 2건 발행 (본인/중복 제외)
        ArgumentCaptor<AlarmNotificationEvent> eventCaptor = ArgumentCaptor.forClass(AlarmNotificationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).extracting(e -> e.receiver().getId())
                .containsExactlyInAnyOrder(11L, 12L);
        assertThat(eventCaptor.getAllValues()).allSatisfy(e ->
                assertThat(e.type()).isEqualTo(AlarmType.JOB));
    }

    @Test
    @DisplayName("create - 졸업생이 아니면 POST_CONTENTS_NOT_GRADUATE 예외")
    void createThrowsWhenNotGraduate() {
        Member student = Member.builder().id(3L).role(MemberRole.STUDENT).build();

        assertThatThrownBy(() -> jobPostService.create(student, createReq()))
                .isInstanceOf(JobPostHandler.class);

        verify(postContentsRepository, never()).save(any());
        verify(jobAlarmRepository, never()).save(any(JobAlarm.class));
    }

    @Test
    @DisplayName("create - 선호 언어 리스트가 엔티티·응답에 그대로 반영된다")
    void createReflectsPreferredLanguages() {
        PostContents withLanguages = PostContents.builder().id(101L)
                .companyName("네이버").jobType(JobType.BACKEND)
                .preferredLanguages(List.of("JavaScript", "React", "Node.js")).build();
        when(postContentsRepository.save(any(PostContents.class))).thenReturn(withLanguages);
        when(graduateRepository.findByMemberId(graduateMember.getId())).thenReturn(Optional.of(graduate));
        when(targetJobRepository.findByInterestedJob(TargetJobCategory.BACKEND)).thenReturn(List.of());

        JobPostResponse response = jobPostService.create(graduateMember, createReq());

        // 빌더에 request.preferredLanguages() 가 전달됐는지 확인
        ArgumentCaptor<PostContents> captor = ArgumentCaptor.forClass(PostContents.class);
        verify(postContentsRepository).save(captor.capture());
        assertThat(captor.getValue().getPreferredLanguages())
                .containsExactly("JavaScript", "React", "Node.js");
        // 응답 매핑(JobPostResponse.from) 확인
        assertThat(response.preferredLanguages())
                .containsExactly("JavaScript", "React", "Node.js");
    }

    @Test
    @DisplayName("getList - 모든 구직 공고 목록을 반환한다")
    void getListReturnsAll() {
        when(postContentsRepository.findAll()).thenReturn(List.of(postContents));

        List<JobPostResponse> result = jobPostService.getList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).companyName()).isEqualTo("네이버");
    }

    @Test
    @DisplayName("getOne - 존재하는 구직 공고를 반환한다")
    void getOneReturnsPost() {
        when(postContentsRepository.findById(100L)).thenReturn(Optional.of(postContents));

        JobPostResponse response = jobPostService.getOne(100L);

        assertThat(response.id()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getOne - 존재하지 않으면 POST_CONTENTS_NOT_FOUND 예외")
    void getOneThrowsWhenNotFound() {
        when(postContentsRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobPostService.getOne(999L))
                .isInstanceOf(JobPostHandler.class);
    }

    @Test
    @DisplayName("update - 작성자(졸업생)가 정상 수정한다")
    void updateByAuthorSucceeds() {
        GraduateJobPost gjp = GraduateJobPost.builder().graduate(graduate).postContents(postContents).build();
        when(postContentsRepository.findById(100L)).thenReturn(Optional.of(postContents));
        when(graduateJobPostRepository.findByPostContentsId(100L)).thenReturn(List.of(gjp));
        when(postContentsRepository.save(any(PostContents.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateJobPostRequest request = new UpdateJobPostRequest(null, null, null, null,
                null, null, null, null, null, null, "카카오");

        JobPostResponse response = jobPostService.update(graduateMember, 100L, request);

        assertThat(response.companyName()).isEqualTo("카카오");
    }

    @Test
    @DisplayName("update - preferredLanguages 는 새 리스트면 교체, null 이면 기존값 유지")
    void updateReplacesOrKeepsPreferredLanguages() {
        GraduateJobPost gjp = GraduateJobPost.builder().graduate(graduate).postContents(postContents).build();
        when(postContentsRepository.findById(100L)).thenReturn(Optional.of(postContents));
        when(graduateJobPostRepository.findByPostContentsId(100L)).thenReturn(List.of(gjp));
        when(postContentsRepository.save(any(PostContents.class))).thenAnswer(inv -> inv.getArgument(0));

        // 새 리스트 전달 → 교체
        UpdateJobPostRequest replaceReq = new UpdateJobPostRequest(null, null, null, null,
                null, null, null, null, null, List.of("Go", "Rust"), null);
        assertThat(jobPostService.update(graduateMember, 100L, replaceReq).preferredLanguages())
                .containsExactly("Go", "Rust");

        // null 전달 → 기존값(setUp: ["Java"]) 유지
        UpdateJobPostRequest keepReq = new UpdateJobPostRequest(null, null, null, null,
                null, null, null, null, null, null, "카카오");
        assertThat(jobPostService.update(graduateMember, 100L, keepReq).preferredLanguages())
                .containsExactly("Java");
    }

    @Test
    @DisplayName("update - jobType 변경 없으면 알람 미발행 (findByPostContentsId 도 호출되지 않음)")
    void updateDoesNotPublishAlarmWhenJobTypeUnchanged() {
        GraduateJobPost gjp = GraduateJobPost.builder().graduate(graduate).postContents(postContents).build();
        when(postContentsRepository.findById(100L)).thenReturn(Optional.of(postContents));
        when(graduateJobPostRepository.findByPostContentsId(100L)).thenReturn(List.of(gjp));
        when(postContentsRepository.save(any(PostContents.class))).thenAnswer(inv -> inv.getArgument(0));

        // jobType 을 기존(BACKEND)과 같게 명시한 요청
        UpdateJobPostRequest sameType = new UpdateJobPostRequest(null, null, null, JobType.BACKEND,
                null, null, null, null, null, null, "카카오");
        jobPostService.update(graduateMember, 100L, sameType);

        // jobType null (= 변경 없음) 요청
        UpdateJobPostRequest nullType = new UpdateJobPostRequest(null, null, null, null,
                null, null, null, null, null, null, "라인");
        jobPostService.update(graduateMember, 100L, nullType);

        verify(jobAlarmRepository, never()).save(any(JobAlarm.class));
        verify(jobAlarmRepository, never()).findByPostContentsId(any());
        verify(targetJobRepository, never()).findByInterestedJob(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("update - jobType 변경 시 prev 만 있고 curr 비면 'Removed' 알람만 발행")
    void updateJobTypeChangePublishesRemovedAlarmOnly() {
        Member studentA = Member.builder().id(11L).nickname("A").build();
        GraduateJobPost gjp = GraduateJobPost.builder().graduate(graduate).postContents(postContents).build();
        // 기존 알람 = A 가 BACKEND 매칭으로 받았던 알람 1건
        JobAlarm aPrev = JobAlarm.builder().id(500L).member(studentA).postContents(postContents)
                .content(JobPostServiceImpl.JOB_ALARM_NEW_CONTENT).isRead(false).build();

        when(postContentsRepository.findById(100L)).thenReturn(Optional.of(postContents));
        when(graduateJobPostRepository.findByPostContentsId(100L)).thenReturn(List.of(gjp));
        when(postContentsRepository.save(any(PostContents.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jobAlarmRepository.findByPostContentsId(100L)).thenReturn(List.of(aPrev));
        // FRONTEND 관심 학생 없음
        when(targetJobRepository.findByInterestedJob(TargetJobCategory.FRONTEND)).thenReturn(List.of());

        UpdateJobPostRequest request = new UpdateJobPostRequest(null, null, null, JobType.FRONTEND,
                null, null, null, null, null, null, null);
        jobPostService.update(graduateMember, 100L, request);

        ArgumentCaptor<JobAlarm> captor = ArgumentCaptor.forClass(JobAlarm.class);
        verify(jobAlarmRepository, times(1)).save(captor.capture());
        JobAlarm dispatched = captor.getValue();
        assertThat(dispatched.getMember().getId()).isEqualTo(11L);
        assertThat(dispatched.getContent()).contains("벗어난");
        assertThat(dispatched.getIsRead()).isFalse();

        // SSE 푸시 이벤트도 'Removed' 대상(A) 1건 발행
        ArgumentCaptor<AlarmNotificationEvent> eventCaptor = ArgumentCaptor.forClass(AlarmNotificationEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().receiver().getId()).isEqualTo(11L);
        assertThat(eventCaptor.getValue().content()).contains("벗어난");
    }

    @Test
    @DisplayName("update - jobType 변경 시 curr 만 있고 prev 비면 'Added' 알람만 발행")
    void updateJobTypeChangePublishesAddedAlarmOnly() {
        Member studentB = Member.builder().id(12L).nickname("B").build();
        GraduateJobPost gjp = GraduateJobPost.builder().graduate(graduate).postContents(postContents).build();
        TargetJob bMatch = TargetJob.builder().id(2L).member(studentB)
                .interestedJob(TargetJobCategory.FRONTEND).build();

        when(postContentsRepository.findById(100L)).thenReturn(Optional.of(postContents));
        when(graduateJobPostRepository.findByPostContentsId(100L)).thenReturn(List.of(gjp));
        when(postContentsRepository.save(any(PostContents.class))).thenAnswer(inv -> inv.getArgument(0));
        // 기존 JobAlarm 없음
        when(jobAlarmRepository.findByPostContentsId(100L)).thenReturn(List.of());
        when(targetJobRepository.findByInterestedJob(TargetJobCategory.FRONTEND)).thenReturn(List.of(bMatch));

        UpdateJobPostRequest request = new UpdateJobPostRequest(null, null, null, JobType.FRONTEND,
                null, null, null, null, null, null, null);
        jobPostService.update(graduateMember, 100L, request);

        ArgumentCaptor<JobAlarm> captor = ArgumentCaptor.forClass(JobAlarm.class);
        verify(jobAlarmRepository, times(1)).save(captor.capture());
        JobAlarm dispatched = captor.getValue();
        assertThat(dispatched.getMember().getId()).isEqualTo(12L);
        assertThat(dispatched.getContent()).contains("새로운 공고");
        assertThat(dispatched.getIsRead()).isFalse();
    }

    @Test
    @DisplayName("update - jobType 변경 시 prev/curr 교집합 보존, Removed/Added 만 새 알람 (등록 졸업생 본인 항상 제외)")
    void updateJobTypeChangePublishesMixedAlarms() {
        Member studentA = Member.builder().id(11L).nickname("A").build();
        Member studentB = Member.builder().id(12L).nickname("B").build();
        Member studentC = Member.builder().id(13L).nickname("C").build();
        Member studentD = Member.builder().id(14L).nickname("D").build();

        GraduateJobPost gjp = GraduateJobPost.builder().graduate(graduate).postContents(postContents).build();

        // prev = {A, B, C} 가 BACKEND 시절 받았던 알람들 (B 는 중복 등록으로 알람 2건 들고 있다고 가정)
        JobAlarm aPrev = JobAlarm.builder().id(501L).member(studentA).postContents(postContents)
                .content(JobPostServiceImpl.JOB_ALARM_NEW_CONTENT).isRead(false).build();
        JobAlarm bPrev1 = JobAlarm.builder().id(502L).member(studentB).postContents(postContents)
                .content(JobPostServiceImpl.JOB_ALARM_NEW_CONTENT).isRead(false).build();
        JobAlarm bPrev2 = JobAlarm.builder().id(503L).member(studentB).postContents(postContents)
                .content(JobPostServiceImpl.JOB_ALARM_NEW_CONTENT).isRead(false).build();
        JobAlarm cPrev = JobAlarm.builder().id(504L).member(studentC).postContents(postContents)
                .content(JobPostServiceImpl.JOB_ALARM_NEW_CONTENT).isRead(false).build();

        // curr = {B, C, D} (자신 등록 졸업생도 FRONTEND 관심으로 등록되어 있다고 가정 → 제외돼야 함)
        TargetJob bCurr = TargetJob.builder().id(20L).member(studentB)
                .interestedJob(TargetJobCategory.FRONTEND).build();
        TargetJob cCurr = TargetJob.builder().id(21L).member(studentC)
                .interestedJob(TargetJobCategory.FRONTEND).build();
        TargetJob dCurr = TargetJob.builder().id(22L).member(studentD)
                .interestedJob(TargetJobCategory.FRONTEND).build();
        TargetJob selfCurr = TargetJob.builder().id(23L).member(graduateMember)
                .interestedJob(TargetJobCategory.FRONTEND).build();

        when(postContentsRepository.findById(100L)).thenReturn(Optional.of(postContents));
        when(graduateJobPostRepository.findByPostContentsId(100L)).thenReturn(List.of(gjp));
        when(postContentsRepository.save(any(PostContents.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jobAlarmRepository.findByPostContentsId(100L)).thenReturn(List.of(aPrev, bPrev1, bPrev2, cPrev));
        when(targetJobRepository.findByInterestedJob(TargetJobCategory.FRONTEND))
                .thenReturn(List.of(bCurr, cCurr, dCurr, selfCurr));

        UpdateJobPostRequest request = new UpdateJobPostRequest(null, null, null, JobType.FRONTEND,
                null, null, null, null, null, null, null);
        jobPostService.update(graduateMember, 100L, request);

        ArgumentCaptor<JobAlarm> captor = ArgumentCaptor.forClass(JobAlarm.class);
        // Removed: A 1건, Added: D 1건 — 총 2건
        verify(jobAlarmRepository, times(2)).save(captor.capture());
        List<JobAlarm> dispatched = captor.getAllValues();

        Set<Long> dispatchedMemberIds = dispatched.stream()
                .map(a -> a.getMember().getId())
                .collect(Collectors.toSet());
        assertThat(dispatchedMemberIds).containsExactlyInAnyOrder(11L, 14L);

        JobAlarm aAlarm = dispatched.stream().filter(a -> a.getMember().getId() == 11L).findFirst().orElseThrow();
        JobAlarm dAlarm = dispatched.stream().filter(a -> a.getMember().getId() == 14L).findFirst().orElseThrow();
        assertThat(aAlarm.getContent()).contains("벗어난");
        assertThat(dAlarm.getContent()).contains("새로운 공고");
        // 등록 졸업생 본인(id=1L) 은 어디에도 포함되지 않음
        assertThat(dispatchedMemberIds).doesNotContain(graduateMember.getId());
    }

    @Test
    @DisplayName("update - 작성자가 아니면 POST_CONTENTS_UNAUTHORIZED 예외")
    void updateThrowsWhenUnauthorized() {
        GraduateJobPost gjp = GraduateJobPost.builder().graduate(graduate).postContents(postContents).build();
        when(postContentsRepository.findById(100L)).thenReturn(Optional.of(postContents));
        when(graduateJobPostRepository.findByPostContentsId(100L)).thenReturn(List.of(gjp));

        UpdateJobPostRequest request = new UpdateJobPostRequest(null, null, null, null,
                null, null, null, null, null, null, "카카오");

        assertThatThrownBy(() -> jobPostService.update(otherMember, 100L, request))
                .isInstanceOf(JobPostHandler.class);
    }

    @Test
    @DisplayName("update - 존재하지 않는 공고면 NOT_FOUND 예외")
    void updateThrowsWhenNotFound() {
        when(postContentsRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateJobPostRequest request = new UpdateJobPostRequest(null, null, null, null,
                null, null, null, null, null, null, "카카오");

        assertThatThrownBy(() -> jobPostService.update(graduateMember, 999L, request))
                .isInstanceOf(JobPostHandler.class);
    }

    @Test
    @DisplayName("delete - 작성자가 정상 삭제하면 JobAlarm/GraduateJobPost/PostContents 순서로 삭제한다")
    void deleteByAuthorSucceeds() {
        GraduateJobPost gjp = GraduateJobPost.builder().graduate(graduate).postContents(postContents).build();
        List<GraduateJobPost> mappings = List.of(gjp);
        when(postContentsRepository.findById(100L)).thenReturn(Optional.of(postContents));
        when(graduateJobPostRepository.findByPostContentsId(100L)).thenReturn(mappings);

        jobPostService.delete(graduateMember, 100L);

        InOrder inOrder = inOrder(jobAlarmRepository, graduateJobPostRepository, postContentsRepository);
        inOrder.verify(jobAlarmRepository).deleteByPostContentsId(100L);
        inOrder.verify(graduateJobPostRepository).deleteAll(mappings);
        inOrder.verify(postContentsRepository).delete(postContents);
    }

    @Test
    @DisplayName("delete - 작성자가 아니면 UNAUTHORIZED 예외 (자식 행도 삭제하지 않음)")
    void deleteThrowsWhenUnauthorized() {
        GraduateJobPost gjp = GraduateJobPost.builder().graduate(graduate).postContents(postContents).build();
        when(postContentsRepository.findById(100L)).thenReturn(Optional.of(postContents));
        when(graduateJobPostRepository.findByPostContentsId(100L)).thenReturn(List.of(gjp));

        assertThatThrownBy(() -> jobPostService.delete(otherMember, 100L))
                .isInstanceOf(JobPostHandler.class);
        verify(jobAlarmRepository, never()).deleteByPostContentsId(any());
        verify(graduateJobPostRepository, never()).deleteAll(any());
        verify(postContentsRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete - 존재하지 않는 공고면 NOT_FOUND 예외")
    void deleteThrowsWhenNotFound() {
        when(postContentsRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobPostService.delete(graduateMember, 999L))
                .isInstanceOf(JobPostHandler.class);
    }

    @Test
    @DisplayName("getMyJobPosts - 졸업생이 등록한 구직 공고 목록을 반환한다")
    void getMyJobPostsReturnsList() {
        GraduateJobPost gjp = GraduateJobPost.builder().graduate(graduate).postContents(postContents).build();
        when(graduateRepository.findByMemberId(graduateMember.getId())).thenReturn(Optional.of(graduate));
        when(graduateJobPostRepository.findByGraduateId(graduate.getId())).thenReturn(List.of(gjp));

        List<JobPostResponse> result = jobPostService.getMyJobPosts(graduateMember);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getMyJobPosts - 졸업생 매핑이 없으면 빈 리스트를 반환한다")
    void getMyJobPostsReturnsEmptyWhenNoGraduate() {
        when(graduateRepository.findByMemberId(otherMember.getId())).thenReturn(Optional.empty());

        List<JobPostResponse> result = jobPostService.getMyJobPosts(otherMember);

        assertThat(result).isEmpty();
    }

    // ── TODO R: 링크 등록 NPE 가드 ───────────────────────────────────

    @Test
    @DisplayName("create - jobType 이 null 이면 알람 분기를 스킵하고 NPE 없이 응답 반환 (TODO R R-2 가드)")
    void createSkipsAlarmDispatchWhenJobTypeNull() {
        PostContents nullJobType = PostContents.builder().id(101L)
                .companyName("(링크 전용)").detailUrl("https://example.com").jobType(null).build();
        when(postContentsRepository.save(any(PostContents.class))).thenReturn(nullJobType);
        when(graduateRepository.findByMemberId(graduateMember.getId())).thenReturn(Optional.of(graduate));

        // create() 가 정상 동작해야 함 (NPE 방지)
        JobPostResponse response = jobPostService.create(graduateMember,
                new CreateJobPostRequest(null, null, null, null, null, null, null, null,
                        "https://example.com", null, null));

        assertThat(response.id()).isEqualTo(101L);
        // 알람 발행 분기 자체가 실행되지 않아야 한다
        verify(targetJobRepository, never()).findByInterestedJob(any());
        verify(jobAlarmRepository, never()).save(any());
    }

    // ── TODO R: 선배/일반 공고 분리 ──────────────────────────────────

    @Test
    @DisplayName("getGraduatePosts - GraduateJobPost 매핑을 GraduatePostResponse 카드로 변환")
    void getGraduatePostsReturnsCards() {
        GraduateJobPost mapping = GraduateJobPost.builder()
                .id(1L).graduate(graduate).postContents(postContents).build();
        when(graduateJobPostRepository.findAll()).thenReturn(List.of(mapping));

        var result = jobPostService.getGraduatePosts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(100L);
        assertThat(result.get(0).graduateMemberId()).isEqualTo(graduateMember.getId());
        assertThat(result.get(0).graduateNickname()).isEqualTo("졸업생");
        assertThat(result.get(0).graduateCareerYear()).isEqualTo(5);
    }

    @Test
    @DisplayName("getCrawledPosts - 선배 매핑이 있는 PostContents id 들을 제외하고 반환")
    void getCrawledPostsExcludesGraduateMappedIds() {
        PostContents crawled = PostContents.builder().id(200L).companyName("크롤링 회사").build();
        when(graduateJobPostRepository.findDistinctPostContentsIds()).thenReturn(List.of(100L));
        when(postContentsRepository.findByIdNotIn(List.of(100L))).thenReturn(List.of(crawled));

        List<JobPostResponse> result = jobPostService.getCrawledPosts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(200L);
        // 매핑이 있을 때는 findAll 을 호출하지 않는다 (불필요 조회 회피)
        verify(postContentsRepository, never()).findAll();
    }

    @Test
    @DisplayName("getCrawledPosts - 선배 매핑이 0건이면 findAll 로 폴백 (IN(빈) SQL 오류 회피)")
    void getCrawledPostsFallsBackToFindAllWhenNoMapping() {
        PostContents crawled1 = PostContents.builder().id(200L).companyName("회사1").build();
        PostContents crawled2 = PostContents.builder().id(201L).companyName("회사2").build();
        when(graduateJobPostRepository.findDistinctPostContentsIds()).thenReturn(List.of());
        when(postContentsRepository.findAll()).thenReturn(List.of(crawled1, crawled2));

        List<JobPostResponse> result = jobPostService.getCrawledPosts();

        assertThat(result).hasSize(2);
        verify(postContentsRepository, never()).findByIdNotIn(any());
    }
}

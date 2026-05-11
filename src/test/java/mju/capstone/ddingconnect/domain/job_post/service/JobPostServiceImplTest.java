package mju.capstone.ddingconnect.domain.job_post.service;

import mju.capstone.ddingconnect.domain.job_post.domain.*;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.GraduateJobPostRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobPostServiceImpl 단위 테스트")
class JobPostServiceImplTest {

    @Mock PostContentsRepository postContentsRepository;
    @Mock GraduateJobPostRepository graduateJobPostRepository;
    @Mock GraduateRepository graduateRepository;

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
                .deadline(LocalDate.of(2026, 6, 30)).build();
    }

    private CreateJobPostRequest createReq() {
        return new CreateJobPostRequest("img", "성남", CareerType.NEW_GRADUATE, JobType.BACKEND,
                "한국", "성남시", "분당구", LocalDate.of(2026, 6, 30),
                "https://test.com", "Java", "네이버");
    }

    @Test
    @DisplayName("create - 졸업생이 구직 공고를 정상 등록한다")
    void create_졸업생_정상등록() {
        when(postContentsRepository.save(any(PostContents.class))).thenReturn(postContents);
        when(graduateRepository.findByMemberId(graduateMember.getId())).thenReturn(Optional.of(graduate));

        JobPostResponse response = jobPostService.create(graduateMember, createReq());

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.companyName()).isEqualTo("네이버");
        verify(postContentsRepository).save(any(PostContents.class));
        verify(graduateJobPostRepository).save(any(GraduateJobPost.class));
    }

    @Test
    @DisplayName("create - 졸업생이 아니면 POST_CONTENTS_NOT_GRADUATE 예외")
    void create_졸업생아님_예외() {
        Member student = Member.builder().id(3L).role(MemberRole.STUDENT).build();

        assertThatThrownBy(() -> jobPostService.create(student, createReq()))
                .isInstanceOf(JobPostHandler.class);

        verify(postContentsRepository, never()).save(any());
    }

    @Test
    @DisplayName("getList - 모든 구직 공고 목록을 반환한다")
    void getList_정상반환() {
        when(postContentsRepository.findAll()).thenReturn(List.of(postContents));

        List<JobPostResponse> result = jobPostService.getList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).companyName()).isEqualTo("네이버");
    }

    @Test
    @DisplayName("getOne - 존재하는 구직 공고를 반환한다")
    void getOne_정상조회() {
        when(postContentsRepository.findById(100L)).thenReturn(Optional.of(postContents));

        JobPostResponse response = jobPostService.getOne(100L);

        assertThat(response.id()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getOne - 존재하지 않으면 POST_CONTENTS_NOT_FOUND 예외")
    void getOne_없음_예외() {
        when(postContentsRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobPostService.getOne(999L))
                .isInstanceOf(JobPostHandler.class);
    }

    @Test
    @DisplayName("update - 작성자(졸업생)가 정상 수정한다")
    void update_정상수정() {
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
    @DisplayName("update - 작성자가 아니면 POST_CONTENTS_UNAUTHORIZED 예외")
    void update_권한없음_예외() {
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
    void update_없음_예외() {
        when(postContentsRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateJobPostRequest request = new UpdateJobPostRequest(null, null, null, null,
                null, null, null, null, null, null, "카카오");

        assertThatThrownBy(() -> jobPostService.update(graduateMember, 999L, request))
                .isInstanceOf(JobPostHandler.class);
    }

    @Test
    @DisplayName("delete - 작성자가 정상 삭제한다")
    void delete_정상삭제() {
        GraduateJobPost gjp = GraduateJobPost.builder().graduate(graduate).postContents(postContents).build();
        when(postContentsRepository.findById(100L)).thenReturn(Optional.of(postContents));
        when(graduateJobPostRepository.findByPostContentsId(100L)).thenReturn(List.of(gjp));

        jobPostService.delete(graduateMember, 100L);

        verify(postContentsRepository).delete(postContents);
    }

    @Test
    @DisplayName("delete - 작성자가 아니면 UNAUTHORIZED 예외")
    void delete_권한없음_예외() {
        GraduateJobPost gjp = GraduateJobPost.builder().graduate(graduate).postContents(postContents).build();
        when(postContentsRepository.findById(100L)).thenReturn(Optional.of(postContents));
        when(graduateJobPostRepository.findByPostContentsId(100L)).thenReturn(List.of(gjp));

        assertThatThrownBy(() -> jobPostService.delete(otherMember, 100L))
                .isInstanceOf(JobPostHandler.class);
        verify(postContentsRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete - 존재하지 않는 공고면 NOT_FOUND 예외")
    void delete_없음_예외() {
        when(postContentsRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobPostService.delete(graduateMember, 999L))
                .isInstanceOf(JobPostHandler.class);
    }
}

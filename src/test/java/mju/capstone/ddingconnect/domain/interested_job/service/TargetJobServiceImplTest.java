package mju.capstone.ddingconnect.domain.interested_job.service;

import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJob;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.interested_job.domain.repository.TargetJobRepository;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.CreateTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.UpdateTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
import mju.capstone.ddingconnect.domain.job_post.domain.PostContents;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.PostContentsRepository;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.response.exception.handler.TargetJobHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TargetJobServiceImpl 단위 테스트")
class TargetJobServiceImplTest {

    @Mock TargetJobRepository targetJobRepository;
    @Mock PostContentsRepository postContentsRepository;
    @InjectMocks TargetJobServiceImpl targetJobService;

    private Member owner;
    private Member other;
    private PostContents postContents;
    private TargetJob targetJob;

    @BeforeEach
    void setUp() {
        owner = Member.builder().id(1L).email("o@mju.ac.kr").nickname("주인").build();
        other = Member.builder().id(2L).email("e@mju.ac.kr").nickname("타인").build();
        postContents = PostContents.builder().id(100L).companyName("네이버").build();
        targetJob = TargetJob.builder().id(10L).member(owner).postContents(postContents)
                .interestedJob(TargetJobCategory.BACKEND).key2("k").build();
    }

    @Test
    @DisplayName("create - 관심 직군을 정상 추가한다")
    void create_정상추가() {
        CreateTargetJobRequest req = new CreateTargetJobRequest(TargetJobCategory.BACKEND, 100L);
        when(postContentsRepository.findById(100L)).thenReturn(Optional.of(postContents));
        when(targetJobRepository.save(any(TargetJob.class))).thenReturn(targetJob);

        TargetJobResponse response = targetJobService.create(owner, req);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.interestedJob()).isEqualTo(TargetJobCategory.BACKEND);
    }

    @Test
    @DisplayName("create - 구직 공고가 없으면 POST_CONTENTS_NOT_FOUND 예외")
    void create_공고없음_예외() {
        CreateTargetJobRequest req = new CreateTargetJobRequest(TargetJobCategory.BACKEND, 999L);
        when(postContentsRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> targetJobService.create(owner, req))
                .isInstanceOf(TargetJobHandler.class);
    }

    @Test
    @DisplayName("getMyTargetJobs - 본인의 관심 직군 목록을 반환한다")
    void getMyTargetJobs_정상반환() {
        when(targetJobRepository.findByMemberId(owner.getId())).thenReturn(List.of(targetJob));

        List<TargetJobResponse> result = targetJobService.getMyTargetJobs(owner);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("update - 본인의 관심 직군을 정상 수정한다")
    void update_정상수정() {
        UpdateTargetJobRequest req = new UpdateTargetJobRequest(TargetJobCategory.FRONTEND);
        when(targetJobRepository.findById(10L)).thenReturn(Optional.of(targetJob));
        when(targetJobRepository.save(any(TargetJob.class))).thenAnswer(inv -> inv.getArgument(0));

        TargetJobResponse response = targetJobService.update(owner, 10L, req);

        assertThat(response.interestedJob()).isEqualTo(TargetJobCategory.FRONTEND);
    }

    @Test
    @DisplayName("update - 본인이 아니면 UNAUTHORIZED 예외")
    void update_권한없음_예외() {
        UpdateTargetJobRequest req = new UpdateTargetJobRequest(TargetJobCategory.FRONTEND);
        when(targetJobRepository.findById(10L)).thenReturn(Optional.of(targetJob));

        assertThatThrownBy(() -> targetJobService.update(other, 10L, req))
                .isInstanceOf(TargetJobHandler.class);
    }

    @Test
    @DisplayName("update - 존재하지 않으면 TARGET_JOB_NOT_FOUND 예외")
    void update_없음_예외() {
        UpdateTargetJobRequest req = new UpdateTargetJobRequest(TargetJobCategory.FRONTEND);
        when(targetJobRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> targetJobService.update(owner, 999L, req))
                .isInstanceOf(TargetJobHandler.class);
    }

    @Test
    @DisplayName("delete - 본인의 관심 직군을 정상 삭제한다")
    void delete_정상삭제() {
        when(targetJobRepository.findById(10L)).thenReturn(Optional.of(targetJob));

        targetJobService.delete(owner, 10L);

        verify(targetJobRepository).delete(targetJob);
    }

    @Test
    @DisplayName("delete - 본인이 아니면 UNAUTHORIZED 예외")
    void delete_권한없음_예외() {
        when(targetJobRepository.findById(10L)).thenReturn(Optional.of(targetJob));

        assertThatThrownBy(() -> targetJobService.delete(other, 10L))
                .isInstanceOf(TargetJobHandler.class);
        verify(targetJobRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete - 존재하지 않으면 NOT_FOUND 예외")
    void delete_없음_예외() {
        when(targetJobRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> targetJobService.delete(owner, 999L))
                .isInstanceOf(TargetJobHandler.class);
    }
}

package mju.capstone.ddingconnect.domain.interested_job.service;

import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJob;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.interested_job.domain.repository.TargetJobRepository;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.CreateTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.UpdateTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
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
    @InjectMocks TargetJobServiceImpl targetJobService;

    private Member owner;
    private Member other;
    private TargetJob targetJob;

    @BeforeEach
    void setUp() {
        owner = Member.builder().id(1L).email("o@mju.ac.kr").nickname("주인").build();
        other = Member.builder().id(2L).email("e@mju.ac.kr").nickname("타인").build();
        targetJob = TargetJob.builder().id(10L).member(owner)
                .interestedJob(TargetJobCategory.BACKEND).key2("k").build();
    }

    @Test
    @DisplayName("create - 관심 직군을 정상 추가한다")
    void create_정상추가() {
        CreateTargetJobRequest req = new CreateTargetJobRequest(TargetJobCategory.BACKEND);
        when(targetJobRepository.existsByMemberIdAndInterestedJob(owner.getId(), TargetJobCategory.BACKEND)).thenReturn(false);
        when(targetJobRepository.save(any(TargetJob.class))).thenReturn(targetJob);

        TargetJobResponse response = targetJobService.create(owner, req);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.interestedJob()).isEqualTo(TargetJobCategory.BACKEND);
    }

    @Test
    @DisplayName("create - 같은 회원이 같은 카테고리로 중복 추가 시 TARGET_JOB_DUPLICATE 예외")
    void create_중복_예외() {
        CreateTargetJobRequest req = new CreateTargetJobRequest(TargetJobCategory.BACKEND);
        when(targetJobRepository.existsByMemberIdAndInterestedJob(owner.getId(), TargetJobCategory.BACKEND)).thenReturn(true);

        assertThatThrownBy(() -> targetJobService.create(owner, req))
                .isInstanceOf(TargetJobHandler.class);
        verify(targetJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - 다른 회원이 같은 카테고리로 추가하는 건 허용")
    void create_다른회원_같은카테고리_허용() {
        CreateTargetJobRequest req = new CreateTargetJobRequest(TargetJobCategory.BACKEND);
        when(targetJobRepository.existsByMemberIdAndInterestedJob(other.getId(), TargetJobCategory.BACKEND)).thenReturn(false);
        when(targetJobRepository.save(any(TargetJob.class))).thenReturn(targetJob);

        TargetJobResponse response = targetJobService.create(other, req);

        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getMyTargetJobs - 본인의 관심 직군 목록을 반환한다")
    void getMyTargetJobs_정상반환() {
        when(targetJobRepository.findByMemberId(owner.getId())).thenReturn(List.of(targetJob));

        List<TargetJobResponse> result = targetJobService.getMyTargetJobs(owner);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("update - 본인의 관심 직군을 다른 카테고리로 정상 수정한다")
    void update_정상수정() {
        UpdateTargetJobRequest req = new UpdateTargetJobRequest(TargetJobCategory.FRONTEND);
        when(targetJobRepository.findById(10L)).thenReturn(Optional.of(targetJob));
        when(targetJobRepository.existsByMemberIdAndInterestedJob(owner.getId(), TargetJobCategory.FRONTEND)).thenReturn(false);
        when(targetJobRepository.save(any(TargetJob.class))).thenAnswer(inv -> inv.getArgument(0));

        TargetJobResponse response = targetJobService.update(owner, 10L, req);

        assertThat(response.interestedJob()).isEqualTo(TargetJobCategory.FRONTEND);
    }

    @Test
    @DisplayName("update - 같은 카테고리로 수정하는 것은 통과한다")
    void update_같은카테고리_통과() {
        UpdateTargetJobRequest req = new UpdateTargetJobRequest(TargetJobCategory.BACKEND);
        when(targetJobRepository.findById(10L)).thenReturn(Optional.of(targetJob));
        when(targetJobRepository.save(any(TargetJob.class))).thenAnswer(inv -> inv.getArgument(0));

        TargetJobResponse response = targetJobService.update(owner, 10L, req);

        assertThat(response.interestedJob()).isEqualTo(TargetJobCategory.BACKEND);
        // 같은 카테고리이므로 existsBy 호출 안 함
        verify(targetJobRepository, never()).existsByMemberIdAndInterestedJob(any(), any());
    }

    @Test
    @DisplayName("update - 이미 보유한 다른 카테고리로 변경 시 TARGET_JOB_DUPLICATE 예외")
    void update_이미보유한_카테고리_예외() {
        UpdateTargetJobRequest req = new UpdateTargetJobRequest(TargetJobCategory.FRONTEND);
        when(targetJobRepository.findById(10L)).thenReturn(Optional.of(targetJob));
        when(targetJobRepository.existsByMemberIdAndInterestedJob(owner.getId(), TargetJobCategory.FRONTEND)).thenReturn(true);

        assertThatThrownBy(() -> targetJobService.update(owner, 10L, req))
                .isInstanceOf(TargetJobHandler.class);
        verify(targetJobRepository, never()).save(any());
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

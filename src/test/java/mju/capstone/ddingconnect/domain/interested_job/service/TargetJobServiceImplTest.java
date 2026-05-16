package mju.capstone.ddingconnect.domain.interested_job.service;

import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJob;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.interested_job.domain.repository.TargetJobRepository;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.ReplaceTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.response.exception.handler.TargetJobHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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

    @BeforeEach
    void setUp() {
        owner = Member.builder().id(1L).email("o@mju.ac.kr").nickname("주인").build();
        other = Member.builder().id(2L).email("e@mju.ac.kr").nickname("타인").build();
    }

    @Test
    @DisplayName("replace - deleteByMemberId 호출 후 입력 개수만큼 save 한다")
    void replace_정상교체() {
        ReplaceTargetJobRequest req = new ReplaceTargetJobRequest(
                List.of(TargetJobCategory.BACKEND, TargetJobCategory.FRONTEND));
        when(targetJobRepository.save(any(TargetJob.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<TargetJobResponse> result = targetJobService.replace(owner, req);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TargetJobResponse::interestedJob)
                .containsExactly(TargetJobCategory.BACKEND, TargetJobCategory.FRONTEND);

        InOrder inOrder = inOrder(targetJobRepository);
        inOrder.verify(targetJobRepository).deleteByMemberId(owner.getId());
        inOrder.verify(targetJobRepository, times(2)).save(any(TargetJob.class));
    }

    @Test
    @DisplayName("replace - 빈 리스트면 본인 row 전부 삭제하고 save 는 호출하지 않는다")
    void replace_빈리스트() {
        ReplaceTargetJobRequest req = new ReplaceTargetJobRequest(List.of());

        List<TargetJobResponse> result = targetJobService.replace(owner, req);

        assertThat(result).isEmpty();
        verify(targetJobRepository).deleteByMemberId(owner.getId());
        verify(targetJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("replace - 입력에 중복 카테고리가 섞이면 dedup 후 1건만 저장한다")
    void replace_중복_dedup() {
        ReplaceTargetJobRequest req = new ReplaceTargetJobRequest(
                List.of(TargetJobCategory.BACKEND, TargetJobCategory.BACKEND));
        when(targetJobRepository.save(any(TargetJob.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<TargetJobResponse> result = targetJobService.replace(owner, req);

        assertThat(result).hasSize(1);
        verify(targetJobRepository, times(1)).save(any(TargetJob.class));
    }

    @Test
    @DisplayName("replace - categories 가 null 이면 _BAD_REQUEST 예외, 삭제·저장 미수행")
    void replace_null_예외() {
        ReplaceTargetJobRequest req = new ReplaceTargetJobRequest(null);

        assertThatThrownBy(() -> targetJobService.replace(owner, req))
                .isInstanceOf(TargetJobHandler.class);
        verify(targetJobRepository, never()).deleteByMemberId(any());
        verify(targetJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("replace - 다른 회원이 호출하면 그 회원 id 로만 deleteByMemberId 한다")
    void replace_다른회원_격리() {
        ReplaceTargetJobRequest req = new ReplaceTargetJobRequest(List.of(TargetJobCategory.DATA));
        when(targetJobRepository.save(any(TargetJob.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        targetJobService.replace(other, req);

        verify(targetJobRepository).deleteByMemberId(other.getId());
        verify(targetJobRepository, never()).deleteByMemberId(owner.getId());
    }

    @Test
    @DisplayName("getMyTargetJobs - 본인의 관심 직군 목록을 반환한다")
    void getMyTargetJobs_정상반환() {
        TargetJob targetJob = TargetJob.builder().id(10L).member(owner)
                .interestedJob(TargetJobCategory.BACKEND).key2("k").build();
        when(targetJobRepository.findByMemberId(owner.getId())).thenReturn(List.of(targetJob));

        List<TargetJobResponse> result = targetJobService.getMyTargetJobs(owner);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).interestedJob()).isEqualTo(TargetJobCategory.BACKEND);
    }
}

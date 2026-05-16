package mju.capstone.ddingconnect.domain.techstack.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStack;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import mju.capstone.ddingconnect.domain.techstack.domain.repository.TechStackRepository;
import mju.capstone.ddingconnect.domain.techstack.dto.request.ReplaceTechStackRequest;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;
import mju.capstone.ddingconnect.global.response.exception.handler.TechStackHandler;
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
@DisplayName("TechStackServiceImpl 단위 테스트")
class TechStackServiceImplTest {

    @Mock TechStackRepository techStackRepository;
    @InjectMocks TechStackServiceImpl techStackService;

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
        ReplaceTechStackRequest req = new ReplaceTechStackRequest(
                List.of(TechStackName.JAVA, TechStackName.SPRING));
        when(techStackRepository.save(any(TechStack.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<TechStackResponse> result = techStackService.replace(owner, req);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TechStackResponse::name)
                .containsExactly(TechStackName.JAVA, TechStackName.SPRING);

        InOrder inOrder = inOrder(techStackRepository);
        inOrder.verify(techStackRepository).deleteByMemberId(owner.getId());
        inOrder.verify(techStackRepository, times(2)).save(any(TechStack.class));
    }

    @Test
    @DisplayName("replace - 빈 리스트면 본인 row 전부 삭제하고 save 는 호출하지 않는다")
    void replace_빈리스트() {
        ReplaceTechStackRequest req = new ReplaceTechStackRequest(List.of());

        List<TechStackResponse> result = techStackService.replace(owner, req);

        assertThat(result).isEmpty();
        verify(techStackRepository).deleteByMemberId(owner.getId());
        verify(techStackRepository, never()).save(any());
    }

    @Test
    @DisplayName("replace - 입력에 중복 enum 이 섞이면 dedup 후 1건만 저장한다")
    void replace_중복_dedup() {
        ReplaceTechStackRequest req = new ReplaceTechStackRequest(
                List.of(TechStackName.JAVA, TechStackName.JAVA));
        when(techStackRepository.save(any(TechStack.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<TechStackResponse> result = techStackService.replace(owner, req);

        assertThat(result).hasSize(1);
        verify(techStackRepository, times(1)).save(any(TechStack.class));
    }

    @Test
    @DisplayName("replace - names 가 null 이면 _BAD_REQUEST 예외, 삭제·저장 미수행")
    void replace_null_예외() {
        ReplaceTechStackRequest req = new ReplaceTechStackRequest(null);

        assertThatThrownBy(() -> techStackService.replace(owner, req))
                .isInstanceOf(TechStackHandler.class);
        verify(techStackRepository, never()).deleteByMemberId(any());
        verify(techStackRepository, never()).save(any());
    }

    @Test
    @DisplayName("replace - 다른 회원이 호출하면 그 회원 id 로만 deleteByMemberId 한다")
    void replace_다른회원_격리() {
        ReplaceTechStackRequest req = new ReplaceTechStackRequest(List.of(TechStackName.PYTHON));
        when(techStackRepository.save(any(TechStack.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        techStackService.replace(other, req);

        verify(techStackRepository).deleteByMemberId(other.getId());
        verify(techStackRepository, never()).deleteByMemberId(owner.getId());
    }

    @Test
    @DisplayName("getMyTechStacks - 본인의 기술 스택 목록을 반환한다")
    void getMyTechStacks_정상반환() {
        TechStack stack = TechStack.builder().id(10L).member(owner).name(TechStackName.JAVA).build();
        when(techStackRepository.findByMemberId(owner.getId())).thenReturn(List.of(stack));

        List<TechStackResponse> result = techStackService.getMyTechStacks(owner);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo(TechStackName.JAVA);
    }
}

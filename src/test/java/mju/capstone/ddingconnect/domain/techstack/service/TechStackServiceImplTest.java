package mju.capstone.ddingconnect.domain.techstack.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStack;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import mju.capstone.ddingconnect.domain.techstack.domain.repository.TechStackRepository;
import mju.capstone.ddingconnect.domain.techstack.dto.request.CreateTechStackRequest;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;
import mju.capstone.ddingconnect.global.response.exception.handler.TechStackHandler;
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
@DisplayName("TechStackServiceImpl 단위 테스트")
class TechStackServiceImplTest {

    @Mock TechStackRepository techStackRepository;
    @InjectMocks TechStackServiceImpl techStackService;

    private Member owner;
    private Member other;
    private TechStack stack;

    @BeforeEach
    void setUp() {
        owner = Member.builder().id(1L).email("o@mju.ac.kr").nickname("주인").build();
        other = Member.builder().id(2L).email("e@mju.ac.kr").nickname("타인").build();
        stack = TechStack.builder().id(10L).member(owner).name(TechStackName.JAVA).build();
    }

    @Test
    @DisplayName("add - 기술 스택을 정상 추가한다")
    void add_정상추가() {
        CreateTechStackRequest req = new CreateTechStackRequest(TechStackName.JAVA);
        when(techStackRepository.existsByMemberIdAndName(owner.getId(), TechStackName.JAVA)).thenReturn(false);
        when(techStackRepository.save(any(TechStack.class))).thenReturn(stack);

        TechStackResponse response = techStackService.add(owner, req);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo(TechStackName.JAVA);
    }

    @Test
    @DisplayName("add - 같은 회원이 같은 이름으로 중복 추가하면 TECH_STACK_DUPLICATE 예외")
    void add_중복_예외() {
        CreateTechStackRequest req = new CreateTechStackRequest(TechStackName.JAVA);
        when(techStackRepository.existsByMemberIdAndName(owner.getId(), TechStackName.JAVA)).thenReturn(true);

        assertThatThrownBy(() -> techStackService.add(owner, req))
                .isInstanceOf(TechStackHandler.class);
        verify(techStackRepository, never()).save(any());
    }

    @Test
    @DisplayName("add - 다른 회원이 같은 이름 등록하는 건 허용")
    void add_다른회원_같은이름_허용() {
        CreateTechStackRequest req = new CreateTechStackRequest(TechStackName.JAVA);
        when(techStackRepository.existsByMemberIdAndName(other.getId(), TechStackName.JAVA)).thenReturn(false);
        when(techStackRepository.save(any(TechStack.class))).thenReturn(stack);

        TechStackResponse response = techStackService.add(other, req);

        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getMyTechStacks - 본인의 기술 스택 목록을 반환한다")
    void getMyTechStacks_정상반환() {
        when(techStackRepository.findByMemberId(owner.getId())).thenReturn(List.of(stack));

        List<TechStackResponse> result = techStackService.getMyTechStacks(owner);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo(TechStackName.JAVA);
    }

    @Test
    @DisplayName("delete - 본인의 기술 스택을 정상 삭제한다")
    void delete_정상삭제() {
        when(techStackRepository.findById(10L)).thenReturn(Optional.of(stack));

        techStackService.delete(owner, 10L);

        verify(techStackRepository).delete(stack);
    }

    @Test
    @DisplayName("delete - 본인이 아니면 UNAUTHORIZED 예외")
    void delete_권한없음_예외() {
        when(techStackRepository.findById(10L)).thenReturn(Optional.of(stack));

        assertThatThrownBy(() -> techStackService.delete(other, 10L))
                .isInstanceOf(TechStackHandler.class);
        verify(techStackRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete - 존재하지 않으면 NOT_FOUND 예외")
    void delete_없음_예외() {
        when(techStackRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> techStackService.delete(owner, 999L))
                .isInstanceOf(TechStackHandler.class);
    }
}

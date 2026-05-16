package mju.capstone.ddingconnect.domain.roadmap.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.roadmap.domain.Roadmap;
import mju.capstone.ddingconnect.domain.roadmap.domain.RoadmapAlarm;
import mju.capstone.ddingconnect.domain.roadmap.domain.repository.RoadmapAlarmRepository;
import mju.capstone.ddingconnect.domain.roadmap.domain.repository.RoadmapRepository;
import mju.capstone.ddingconnect.domain.roadmap.dto.request.CreateRoadmapRequest;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapResponse;
import mju.capstone.ddingconnect.global.response.exception.handler.RoadmapHandler;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoadmapServiceImpl 단위 테스트")
class RoadmapServiceImplTest {

    @Mock RoadmapRepository roadmapRepository;
    @Mock RoadmapAlarmRepository roadmapAlarmRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks RoadmapServiceImpl roadmapService;

    private Member author;
    private Member other;
    private Roadmap roadmap;

    @BeforeEach
    void setUp() {
        author = Member.builder().id(1L).email("a@mju.ac.kr").nickname("작성자").build();
        other = Member.builder().id(2L).email("o@mju.ac.kr").nickname("타인").build();
        roadmap = Roadmap.builder().id(10L).member(author).content("백엔드 개발자 로드맵 본문").build();
    }

    @Test
    @DisplayName("create - 로드맵을 정상 등록하고 본인에게 RoadmapAlarm 1건 발행한다")
    void create_정상등록_알람발행() {
        CreateRoadmapRequest req = new CreateRoadmapRequest("백엔드 개발자 로드맵 본문");
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(roadmap);

        RoadmapResponse response = roadmapService.create(author, req);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.content()).isEqualTo("백엔드 개발자 로드맵 본문");
        verify(roadmapAlarmRepository).save(any(RoadmapAlarm.class));

        // 커밋 후 SSE 푸시용 이벤트가 로드맵 생성자 본인 대상으로 발행된다
        ArgumentCaptor<AlarmNotificationEvent> eventCaptor = ArgumentCaptor.forClass(AlarmNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        AlarmNotificationEvent event = eventCaptor.getValue();
        assertThat(event.receiver().getId()).isEqualTo(author.getId());
        assertThat(event.type()).isEqualTo(AlarmType.ROADMAP);
        assertThat(event.content()).isEqualTo(RoadmapServiceImpl.ROADMAP_ALARM_CONTENT);
    }

    @Test
    @DisplayName("create - JSON 형식이 아닌 일반 문자열도 형식 제한 없이 정상 등록된다")
    void create_일반문자열_정상등록() {
        // 이전에는 JSON object/array만 허용했으나, 이제는 일반 문자열도 그대로 통과해야 한다
        CreateRoadmapRequest req = new CreateRoadmapRequest("Java 부터 차근차근 학습하세요");
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(roadmap);

        roadmapService.create(author, req);

        verify(roadmapRepository).save(any(Roadmap.class));
    }

    @Test
    @DisplayName("create - content가 null 또는 공백이면 INVALID_CONTENT 예외")
    void create_blankContent_예외() {
        assertThatThrownBy(() -> roadmapService.create(author, new CreateRoadmapRequest(null)))
                .isInstanceOf(RoadmapHandler.class);
        assertThatThrownBy(() -> roadmapService.create(author, new CreateRoadmapRequest("   ")))
                .isInstanceOf(RoadmapHandler.class);
        verify(roadmapRepository, never()).save(any(Roadmap.class));
    }

    @Test
    @DisplayName("getList - 로드맵 목록을 반환한다")
    void getList_정상반환() {
        when(roadmapRepository.findAll()).thenReturn(List.of(roadmap));

        List<RoadmapResponse> result = roadmapService.getList();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getOne - 존재하는 로드맵을 반환한다")
    void getOne_정상조회() {
        when(roadmapRepository.findById(10L)).thenReturn(Optional.of(roadmap));

        RoadmapResponse response = roadmapService.getOne(10L);

        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getOne - 존재하지 않으면 NOT_FOUND 예외")
    void getOne_없음_예외() {
        when(roadmapRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roadmapService.getOne(999L))
                .isInstanceOf(RoadmapHandler.class);
    }

    @Test
    @DisplayName("delete - 작성자가 정상 삭제하면 RoadmapAlarm 먼저 삭제 후 Roadmap 삭제")
    void delete_정상삭제() {
        when(roadmapRepository.findById(10L)).thenReturn(Optional.of(roadmap));

        roadmapService.delete(author, 10L);

        InOrder inOrder = inOrder(roadmapAlarmRepository, roadmapRepository);
        inOrder.verify(roadmapAlarmRepository).deleteByRoadmapId(10L);
        inOrder.verify(roadmapRepository).delete(roadmap);
    }

    @Test
    @DisplayName("delete - 작성자가 아니면 UNAUTHORIZED 예외, 자식/본체 모두 미삭제")
    void delete_권한없음_예외() {
        when(roadmapRepository.findById(10L)).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> roadmapService.delete(other, 10L))
                .isInstanceOf(RoadmapHandler.class);
        verify(roadmapAlarmRepository, never()).deleteByRoadmapId(any());
        verify(roadmapRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete - 존재하지 않으면 NOT_FOUND 예외")
    void delete_없음_예외() {
        when(roadmapRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roadmapService.delete(author, 999L))
                .isInstanceOf(RoadmapHandler.class);
    }
}

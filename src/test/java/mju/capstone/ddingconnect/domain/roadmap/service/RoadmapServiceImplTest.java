package mju.capstone.ddingconnect.domain.roadmap.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.roadmap.domain.Roadmap;
import mju.capstone.ddingconnect.domain.roadmap.domain.repository.RoadmapRepository;
import mju.capstone.ddingconnect.domain.roadmap.dto.request.CreateRoadmapRequest;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapResponse;
import mju.capstone.ddingconnect.global.response.exception.handler.RoadmapHandler;
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
@DisplayName("RoadmapServiceImpl 단위 테스트")
class RoadmapServiceImplTest {

    @Mock RoadmapRepository roadmapRepository;
    @InjectMocks RoadmapServiceImpl roadmapService;

    private Member author;
    private Member other;
    private Roadmap roadmap;

    @BeforeEach
    void setUp() {
        author = Member.builder().id(1L).email("a@mju.ac.kr").nickname("작성자").build();
        other = Member.builder().id(2L).email("o@mju.ac.kr").nickname("타인").build();
        roadmap = Roadmap.builder().id(10L).member(author).content("{\"step\":\"1\"}").build();
    }

    @Test
    @DisplayName("create - 로드맵을 정상 등록한다")
    void create_정상등록() {
        CreateRoadmapRequest req = new CreateRoadmapRequest("{\"step\":\"1\"}");
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(roadmap);

        RoadmapResponse response = roadmapService.create(author, req);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.content()).contains("step");
    }

    @Test
    @DisplayName("create - JSON array도 정상 등록된다")
    void create_jsonArray_정상등록() {
        CreateRoadmapRequest req = new CreateRoadmapRequest("[{\"step\":1},{\"step\":2}]");
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(roadmap);

        roadmapService.create(author, req);

        verify(roadmapRepository).save(any(Roadmap.class));
    }

    @Test
    @DisplayName("create - content가 invalid JSON이면 INVALID_CONTENT 예외")
    void create_invalidJson_예외() {
        CreateRoadmapRequest req = new CreateRoadmapRequest("JAVA");

        assertThatThrownBy(() -> roadmapService.create(author, req))
                .isInstanceOf(RoadmapHandler.class);
        verify(roadmapRepository, never()).save(any(Roadmap.class));
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
    @DisplayName("create - JSON primitive(문자열/숫자/불리언/null)은 거부된다")
    void create_jsonPrimitive_예외() {
        assertThatThrownBy(() -> roadmapService.create(author, new CreateRoadmapRequest("\"JAVA\"")))
                .isInstanceOf(RoadmapHandler.class);
        assertThatThrownBy(() -> roadmapService.create(author, new CreateRoadmapRequest("123")))
                .isInstanceOf(RoadmapHandler.class);
        assertThatThrownBy(() -> roadmapService.create(author, new CreateRoadmapRequest("true")))
                .isInstanceOf(RoadmapHandler.class);
        assertThatThrownBy(() -> roadmapService.create(author, new CreateRoadmapRequest("null")))
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
    @DisplayName("delete - 작성자가 정상 삭제한다")
    void delete_정상삭제() {
        when(roadmapRepository.findById(10L)).thenReturn(Optional.of(roadmap));

        roadmapService.delete(author, 10L);

        verify(roadmapRepository).delete(roadmap);
    }

    @Test
    @DisplayName("delete - 작성자가 아니면 UNAUTHORIZED 예외")
    void delete_권한없음_예외() {
        when(roadmapRepository.findById(10L)).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> roadmapService.delete(other, 10L))
                .isInstanceOf(RoadmapHandler.class);
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

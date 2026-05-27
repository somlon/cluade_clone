package mju.capstone.ddingconnect.domain.roadmap.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import mju.capstone.ddingconnect.domain.roadmap.domain.Roadmap;

import java.time.LocalDateTime;

/**
 * 로드맵 목록 카드 화면 전용 경량 응답 DTO.
 * <p>
 * 본문 {@code content}(JSON) 통째로 내려보내지 않고 카드 렌더에 필요한 필드만 추출해 페이로드를 줄인다.
 * 상세 조회·생성 응답은 기존 {@link RoadmapResponse} 가 그대로 담당한다.
 *
 * @param id        로드맵 ID — 다운로드 버튼 클릭 시 {@code GET /api/v1/roadmaps/{id}/download} 호출에 사용
 * @param title     {@code content} JSON 의 {@code roadmap_title} 만 추출. 추출 실패·빈 제목 시 폴백 {@code "로드맵"}
 * @param createdAt 생성 시각 — 카드의 날짜·시간 표시용 (포맷은 프론트 담당)
 */
public record RoadmapListResponse(
        Long id,
        String title,
        LocalDateTime createdAt
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_TITLE = "로드맵";

    public static RoadmapListResponse from(Roadmap roadmap) {
        return new RoadmapListResponse(
                roadmap.getId(),
                extractTitle(roadmap.getContent()),
                roadmap.getCreatedAt()
        );
    }

    private static String extractTitle(String content) {
        if (content == null || content.isBlank()) return DEFAULT_TITLE;
        try {
            String title = MAPPER.readTree(content).path("roadmap_title").asText("");
            return title.isBlank() ? DEFAULT_TITLE : title;
        } catch (Exception e) {
            return DEFAULT_TITLE;
        }
    }
}

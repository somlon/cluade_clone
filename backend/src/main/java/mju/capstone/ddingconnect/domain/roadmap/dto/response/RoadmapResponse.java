package mju.capstone.ddingconnect.domain.roadmap.dto.response;

import mju.capstone.ddingconnect.domain.roadmap.domain.Roadmap;

import java.time.LocalDateTime;

public record RoadmapResponse(
        Long id,
        Long memberId,
        String content,
        LocalDateTime createdAt   // 생성 시각 — 마이 로드맵 목록의 날짜 표시용
) {
    public static RoadmapResponse from(Roadmap roadmap) {
        return new RoadmapResponse(
                roadmap.getId(),
                roadmap.getMember().getId(),
                roadmap.getContent(),
                roadmap.getCreatedAt()
        );
    }
}

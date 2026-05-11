package mju.capstone.ddingconnect.domain.roadmap.dto.response;

import mju.capstone.ddingconnect.domain.roadmap.domain.Roadmap;

public record RoadmapResponse(
        Long id,
        Long memberId,
        String content
) {
    public static RoadmapResponse from(Roadmap roadmap) {
        return new RoadmapResponse(
                roadmap.getId(),
                roadmap.getMember().getId(),
                roadmap.getContent()
        );
    }
}

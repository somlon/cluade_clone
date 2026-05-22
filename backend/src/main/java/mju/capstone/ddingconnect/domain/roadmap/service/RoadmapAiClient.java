package mju.capstone.ddingconnect.domain.roadmap.service;

import mju.capstone.ddingconnect.domain.roadmap.dto.request.CreateRoadmapRequest;

/**
 * [로드맵 AI 생성 클라이언트]
 *
 * 로드맵 입력 폼을 데이터 파트({@code ddingconnect-data}) 의
 * {@code POST /api/data/generate} 로 전달하고, AI 가 생성한 로드맵 JSON 문자열을
 * 받아오는 아웃바운드 클라이언트.
 *
 * <p>데이터 파트는 AI 생성만 담당하며 저장·조회·알람은 백엔드가 맡는다
 * (커피챗 {@code MatchingAlgorithmClient} 와 동일한 얇은 클라이언트 패턴).
 */
public interface RoadmapAiClient {

    /**
     * 데이터 파트 AI 를 호출해 로드맵 본문(JSON 문자열)을 생성한다.
     *
     * @param form     로드맵 입력 폼 6필드
     * @param memberId 회원 ID — 데이터 파트 {@code member_id} URL 쿼리 파라미터로 전달
     * @return AI 가 생성한 로드맵 JSON 문자열
     */
    String generate(CreateRoadmapRequest form, Long memberId);
}

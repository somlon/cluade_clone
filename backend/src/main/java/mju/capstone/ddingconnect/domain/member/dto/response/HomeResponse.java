package mju.capstone.ddingconnect.domain.member.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;

/**
 * [홈 화면 조회 응답 DTO]
 * 홈 화면을 1회 호출로 렌더링하기 위한 통합 응답.
 *
 * 역할별 학과/학년 자리 표시 — STUDENT 와 GRADUATE 가 같은 UI 슬롯을 서로 다른 키로 채운다:
 * - 학과 자리 = STUDENT 는 {@code department}, GRADUATE 는 {@code company}
 * - 학년 자리 = STUDENT 는 {@code grade},      GRADUATE 는 {@code careerYear}
 *
 * 비해당 역할 필드는 모두 null 로 채워 {@code @JsonInclude(NON_NULL)} 가
 * 응답 JSON 에서 키 자체를 제외한다 → 프론트는 역할 분기 없이 키 존재 여부로 안전 렌더 가능.
 *
 * 의미 분리: null = 비해당 역할(키 자체 제외), 0 = 해당 역할 + 0개(키 유지).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HomeResponse(
        Long point,
        String nickname,
        String department,
        String company,
        MemberRole role,
        Integer grade,
        Integer careerYear,
        ActivityCounts activity
) {

    /**
     * 나의 활동 카드 카운트 (마이페이지 활동 통계와 동일 집계 기준 재사용).
     *
     * @param coffeeChatCount 본인이 요청자/수신자로 참여한 수락(ACCEPTED) 커피챗 수
     * @param roadmapCount    본인이 생성한 로드맵 수 (STUDENT 전용 — GRADUATE 는 null → 응답 제외)
     * @param questionCount   본인이 작성한 질문 수 (QnA)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ActivityCounts(
            long coffeeChatCount,
            Long roadmapCount,
            long questionCount
    ) {}
}

package mju.capstone.ddingconnect.domain.member.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import mju.capstone.ddingconnect.domain.job_post.domain.JobType;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;

/**
 * [홈 화면 조회 응답 DTO]
 * 홈 화면을 1회 호출로 렌더링하기 위한 통합 응답.
 * - point     : 우측 상단 포인트 뱃지 (단위 P)
 * - nickname  : "{닉네임}, 안녕하세요" 인사 영역
 * - department: 학과 표기 ("컴퓨터공학과")
 * - role      : STUDENT/GRADUATE 분기에 사용
 * - grade     : STUDENT 전용 ("3학년") — GRADUATE 는 null → 응답에서 키 자체 제외
 * - career    : GRADUATE 전용 (jobType/company/careerYear) — STUDENT 는 null → 응답에서 키 자체 제외
 * - activity  : 나의 활동 카운트 (커피챗/로드맵/QnA) — 마이페이지(MyPageResponse.ActivityStats)와 동일 집계 기준 재사용
 *
 * 의미 분리: null = 비해당 역할(키 자체 제외), 0 = 해당 역할 + 0개(키 유지).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HomeResponse(
        Long point,
        String nickname,
        String department,
        MemberRole role,
        Integer grade,
        CareerInfo career,
        ActivityCounts activity
) {

    /**
     * 졸업생 경력 정보 (학년 자리 대체).
     * STUDENT 는 이 객체 자체가 null → 응답에서 키 자체 제외.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CareerInfo(
            JobType jobType,
            String company,
            Integer careerYear
    ) {}

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

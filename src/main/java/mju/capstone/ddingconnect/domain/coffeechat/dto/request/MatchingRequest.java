package mju.capstone.ddingconnect.domain.coffeechat.dto.request;

/**
 * [커피챗 매칭 요청 DTO]
 * 매칭 정보 입력 화면(화면 1)의 폼 6필드.
 * 매칭 알고리즘(타 파트)으로 그대로 전달되며 백엔드에 저장되지 않는다.
 * @param grade 현재 학년
 * @param gpa 학점
 * @param major 전공
 * @param interestedJob 관심 직무
 * @param capability 보유 역량
 * @param targetCompany 목표 기업
 */
public record MatchingRequest(
        Integer grade,
        String gpa,
        String major,
        String interestedJob,
        String capability,
        String targetCompany
) {}

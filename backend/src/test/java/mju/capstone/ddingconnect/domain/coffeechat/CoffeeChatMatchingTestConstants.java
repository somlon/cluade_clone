package mju.capstone.ddingconnect.domain.coffeechat;

import mju.capstone.ddingconnect.domain.coffeechat.dto.request.MatchingRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MatchedCandidateDetailResponse;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MatchedCandidateResponse;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;

import java.util.List;

/**
 * [커피챗 매칭 테스트 공유 픽스처]
 * CoffeeChatMatchingControllerTest / CoffeeChatMatchingServiceImplTest 가 공유하는
 * 엔드포인트 경로·매칭 폼 입력값·후보 회원 ID·응답 DTO 픽스처를 한곳에서 관리한다(하드코딩 금지).
 */
public final class CoffeeChatMatchingTestConstants {

    private CoffeeChatMatchingTestConstants() {}

    // 엔드포인트
    public static final String BASE_URL = "/api/v1/coffeechat";
    public static final String MATCHING_URL = BASE_URL + "/matching";

    // 매칭 폼 6필드
    public static final Integer FORM_GRADE = 3;
    public static final String FORM_GPA = "4.0";
    public static final String FORM_MAJOR = "응용소프트웨어학과";
    public static final String FORM_INTERESTED_JOB = "백엔드 개발자";
    public static final String FORM_CAPABILITY = "Spring Boot, JPA";
    public static final String FORM_TARGET_COMPANY = "카카오";

    // 후보 회원 ID
    public static final Long CANDIDATE_ID_1 = 101L;
    public static final Long CANDIDATE_ID_2 = 102L;
    public static final Long CANDIDATE_ID_3 = 103L;

    // 후보 프로필 더미값 (테스트는 memberId 만 검증, 나머지는 placeholder)
    private static final String CANDIDATE_NICKNAME = "매칭후보";
    private static final String CANDIDATE_DEPARTMENT = "응용소프트웨어학과";

    /** 매칭 폼 6필드를 모두 채운 샘플 요청 */
    public static MatchingRequest sampleForm() {
        return new MatchingRequest(FORM_GRADE, FORM_GPA, FORM_MAJOR,
                FORM_INTERESTED_JOB, FORM_CAPABILITY, FORM_TARGET_COMPANY);
    }

    /** memberId 만 의미 있는 매칭 후보 카드 픽스처 */
    public static MatchedCandidateResponse candidateCard(Long memberId) {
        return new MatchedCandidateResponse(memberId, MemberRole.GRADUATE,
                CANDIDATE_NICKNAME, CANDIDATE_DEPARTMENT,
                List.of(), List.of(), null, null, null, null, null);
    }

    /** memberId 만 의미 있는 매칭 후보 상세 픽스처 */
    public static MatchedCandidateDetailResponse candidateDetail(Long memberId) {
        return new MatchedCandidateDetailResponse(memberId, MemberRole.GRADUATE,
                CANDIDATE_NICKNAME, CANDIDATE_DEPARTMENT,
                List.of(), List.of(), null, null, null, null, null,
                null, null, null, null, null);
    }
}

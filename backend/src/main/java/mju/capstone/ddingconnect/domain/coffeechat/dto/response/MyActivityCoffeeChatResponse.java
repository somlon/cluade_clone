package mju.capstone.ddingconnect.domain.coffeechat.dto.response;

import mju.capstone.ddingconnect.domain.job_post.domain.JobType;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;

import java.util.List;

/**
 * ["나의 활동 › 커피챗" 카드 응답 DTO]
 * 마이페이지 '나의 활동 › 커피챗' 카드 UI 전용 경량 응답.
 * 매칭 카드(MatchedCandidateResponse)·상세(MatchedCandidateDetailResponse)와 분리해
 * 화면이 실제로 쓰는 8필드만 노출한다(페이로드 절감 + 책임 분리).
 *
 * <p>getMyActivity 의 상대(receiver)는 항상 졸업생이므로 GRADUATE 부가 정보
 * (enrollmentYear · company · jobType · careerYear)를 포함한다.
 */
public record MyActivityCoffeeChatResponse(
        Long memberId,
        String nickname,
        String department,
        String enrollmentYear,
        String company,
        JobType jobType,
        Integer careerYear,
        List<TechStackName> techStacks
) {}

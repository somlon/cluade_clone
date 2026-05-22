package mju.capstone.ddingconnect.domain.roadmap.dto.request;

import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;

import java.util.List;

/**
 * [로드맵 생성 요청 DTO]
 * 로드맵 입력 폼 6필드. 데이터 파트 AI({@code POST /api/data/generate})로 전달돼
 * AI 로드맵이 생성되며, 결과는 백엔드 {@code Roadmap.content} 에 저장된다.
 *
 * @param grade         현재 학년
 * @param gpa           현재 학점
 * @param major         전공
 * @param targetJob     관심 직군
 * @param currentSkills 보유 기술 스택
 * @param targetCompany 목표 기업
 */
public record CreateRoadmapRequest(
        Integer grade,
        Double gpa,
        String major,
        TargetJobCategory targetJob,
        List<TechStackName> currentSkills,
        String targetCompany
) {}

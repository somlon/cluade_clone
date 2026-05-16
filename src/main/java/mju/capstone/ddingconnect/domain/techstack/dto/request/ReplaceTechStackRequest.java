package mju.capstone.ddingconnect.domain.techstack.dto.request;

import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;

import java.util.List;

/**
 * [기술 스택 일괄 교체 요청 DTO]
 * @param names 교체할 기술 스택 전체 리스트. null 이면 400, 빈 리스트는 전부 삭제를 의미한다.
 */
public record ReplaceTechStackRequest(
        List<TechStackName> names
) {}

package mju.capstone.ddingconnect.domain.alarm.dto.response;

import mju.capstone.ddingconnect.domain.alarm.domain.AlarmType;

import java.time.LocalDateTime;

/**
 * [통합 알람 응답 DTO]
 * 4종 알람(답변/구직/로드맵/커피챗)을 단일 형식으로 표현
 *
 * @param type         알람 종류 (ENUM)
 * @param id           해당 알람 테이블의 PK
 * @param refId        원본 엔티티 PK (답변/구직공고/로드맵/커피챗의 PK)
 * @param content      알람 내용
 * @param isRead       읽음 여부
 * @param createdAt    알람 생성 시간 (BaseEntity.createdAt)
 * @param relativeTime "방금 전 / N분 전 / N시간 전 / N일 전 / N개월 전 / N년 전" 표시용 문자열.
 *                     응답 생성 시 createdAt 으로부터 계산되므로 매 조회마다 자동 갱신됨.
 */
public record AlarmResponse(
        AlarmType type,
        Long id,
        Long refId,
        String content,
        Boolean isRead,
        LocalDateTime createdAt,
        String relativeTime
) {}

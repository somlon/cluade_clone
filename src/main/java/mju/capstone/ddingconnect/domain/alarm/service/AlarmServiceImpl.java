package mju.capstone.ddingconnect.domain.alarm.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.alarm.domain.AlarmType;
import mju.capstone.ddingconnect.domain.alarm.dto.response.AlarmResponse;
import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatAlarm;
import mju.capstone.ddingconnect.domain.coffeechat.domain.repository.CoffeeChatAlarmRepository;
import mju.capstone.ddingconnect.domain.job_post.domain.JobAlarm;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.JobAlarmRepository;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.qna.answer.domain.AnswerAlarm;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerAlarmRepository;
import mju.capstone.ddingconnect.domain.roadmap.domain.RoadmapAlarm;
import mju.capstone.ddingconnect.domain.roadmap.domain.repository.RoadmapAlarmRepository;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.AlarmHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * [통합 알람 서비스 구현체]
 * 4종 알람 Repository를 모두 호출해 단일 응답 리스트로 합쳐 반환
 */
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements AlarmService {

    private final AnswerAlarmRepository answerAlarmRepository;
    private final JobAlarmRepository jobAlarmRepository;
    private final RoadmapAlarmRepository roadmapAlarmRepository;
    private final CoffeeChatAlarmRepository coffeeChatAlarmRepository;

    /* ==================== 목록 조회 ==================== */

    @Override
    @Transactional(readOnly = true)
    public List<AlarmResponse> getMyAlarms(Member member) {
        Long memberId = member.getId();
        List<AlarmResponse> result = new ArrayList<>();

        // 1. 답변 알람 (내 질문에 답변이 달렸을 때)
        answerAlarmRepository.findByQuestionOwnerId(memberId)
                .forEach(a -> result.add(toAnswerResponse(a)));

        // 2. 구직 알람 (회원에게 직접 발송)
        jobAlarmRepository.findByMemberId(memberId)
                .forEach(a -> result.add(toJobResponse(a)));

        // 3. 로드맵 알람 (내 로드맵 관련 알람)
        roadmapAlarmRepository.findByRoadmapOwnerId(memberId)
                .forEach(a -> result.add(toRoadmapResponse(a)));

        // 4. 커피챗 알람 (요청자/수신자 각각 독립 발행 → member FK로 직접 조회)
        coffeeChatAlarmRepository.findByMemberId(memberId)
                .forEach(a -> result.add(toCoffeeChatResponse(a)));

        // 시간 내림차순 정렬 (createdAt이 null인 항목은 가장 뒤로)
        result.sort(Comparator.comparing(
                AlarmResponse::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return result;
    }

    /* ==================== 상세 조회 ==================== */

    @Override
    @Transactional(readOnly = true)
    public AlarmResponse getAlarmDetail(Member member, AlarmType type, Long alarmId) {
        Long memberId = member.getId();
        return switch (type) {
            case ANSWER -> {
                AnswerAlarm a = findAnswerAlarm(alarmId);
                verifyAnswerAlarmOwner(a, memberId);
                yield toAnswerResponse(a);
            }
            case JOB -> {
                JobAlarm a = findJobAlarm(alarmId);
                verifyJobAlarmOwner(a, memberId);
                yield toJobResponse(a);
            }
            case ROADMAP -> {
                RoadmapAlarm a = findRoadmapAlarm(alarmId);
                verifyRoadmapAlarmOwner(a, memberId);
                yield toRoadmapResponse(a);
            }
            case COFFEE_CHAT -> {
                CoffeeChatAlarm a = findCoffeeChatAlarm(alarmId);
                verifyCoffeeChatAlarmOwner(a, memberId);
                yield toCoffeeChatResponse(a);
            }
        };
    }

    /* ==================== 읽음 처리 ==================== */

    @Override
    @Transactional
    public void markAsRead(Member member, AlarmType type, Long alarmId) {
        Long memberId = member.getId();
        switch (type) {
            case ANSWER -> {
                AnswerAlarm a = findAnswerAlarm(alarmId);
                verifyAnswerAlarmOwner(a, memberId);
                AnswerAlarm updated = AnswerAlarm.builder()
                        .id(a.getId())
                        .answer(a.getAnswer())
                        .content(a.getContent())
                        .isRead(true)
                        .build();
                answerAlarmRepository.save(updated);
            }
            case JOB -> {
                JobAlarm a = findJobAlarm(alarmId);
                verifyJobAlarmOwner(a, memberId);
                JobAlarm updated = JobAlarm.builder()
                        .id(a.getId())
                        .member(a.getMember())
                        .postContents(a.getPostContents())
                        .content(a.getContent())
                        .isRead(true)
                        .build();
                jobAlarmRepository.save(updated);
            }
            case ROADMAP -> {
                RoadmapAlarm a = findRoadmapAlarm(alarmId);
                verifyRoadmapAlarmOwner(a, memberId);
                RoadmapAlarm updated = RoadmapAlarm.builder()
                        .id(a.getId())
                        .roadmap(a.getRoadmap())
                        .content(a.getContent())
                        .isRead(true)
                        .build();
                roadmapAlarmRepository.save(updated);
            }
            case COFFEE_CHAT -> {
                CoffeeChatAlarm a = findCoffeeChatAlarm(alarmId);
                verifyCoffeeChatAlarmOwner(a, memberId);
                CoffeeChatAlarm updated = CoffeeChatAlarm.builder()
                        .id(a.getId())
                        .coffeeChat(a.getCoffeeChat())
                        .member(a.getMember())
                        .content(a.getContent())
                        .isRead(true)
                        .build();
                coffeeChatAlarmRepository.save(updated);
            }
        }
    }

    /* ==================== 내부 조회 헬퍼 ==================== */

    private AnswerAlarm findAnswerAlarm(Long id) {
        return answerAlarmRepository.findById(id)
                .orElseThrow(() -> new AlarmHandler(ErrorStatus.ALARM_NOT_FOUND));
    }

    private JobAlarm findJobAlarm(Long id) {
        return jobAlarmRepository.findById(id)
                .orElseThrow(() -> new AlarmHandler(ErrorStatus.ALARM_NOT_FOUND));
    }

    private RoadmapAlarm findRoadmapAlarm(Long id) {
        return roadmapAlarmRepository.findById(id)
                .orElseThrow(() -> new AlarmHandler(ErrorStatus.ALARM_NOT_FOUND));
    }

    private CoffeeChatAlarm findCoffeeChatAlarm(Long id) {
        return coffeeChatAlarmRepository.findById(id)
                .orElseThrow(() -> new AlarmHandler(ErrorStatus.ALARM_NOT_FOUND));
    }

    /* ==================== 권한 검증 헬퍼 ==================== */

    private void verifyAnswerAlarmOwner(AnswerAlarm a, Long memberId) {
        if (!a.getAnswer().getQuestion().getMember().getId().equals(memberId)) {
            throw new AlarmHandler(ErrorStatus.ALARM_UNAUTHORIZED);
        }
    }

    private void verifyJobAlarmOwner(JobAlarm a, Long memberId) {
        if (!a.getMember().getId().equals(memberId)) {
            throw new AlarmHandler(ErrorStatus.ALARM_UNAUTHORIZED);
        }
    }

    private void verifyRoadmapAlarmOwner(RoadmapAlarm a, Long memberId) {
        if (!a.getRoadmap().getMember().getId().equals(memberId)) {
            throw new AlarmHandler(ErrorStatus.ALARM_UNAUTHORIZED);
        }
    }

    private void verifyCoffeeChatAlarmOwner(CoffeeChatAlarm a, Long memberId) {
        if (!a.getMember().getId().equals(memberId)) {
            throw new AlarmHandler(ErrorStatus.ALARM_UNAUTHORIZED);
        }
    }

    /* ==================== 응답 변환 헬퍼 ==================== */

    private AlarmResponse toAnswerResponse(AnswerAlarm a) {
        return new AlarmResponse(
                AlarmType.ANSWER, a.getId(),
                a.getAnswer().getId(), a.getContent(),
                a.getIsRead(), a.getCreatedAt()
        );
    }

    private AlarmResponse toJobResponse(JobAlarm a) {
        return new AlarmResponse(
                AlarmType.JOB, a.getId(),
                a.getPostContents().getId(), a.getContent(),
                a.getIsRead(), a.getCreatedAt()
        );
    }

    private AlarmResponse toRoadmapResponse(RoadmapAlarm a) {
        return new AlarmResponse(
                AlarmType.ROADMAP, a.getId(),
                a.getRoadmap().getId(), a.getContent(),
                a.getIsRead(), a.getCreatedAt()
        );
    }

    private AlarmResponse toCoffeeChatResponse(CoffeeChatAlarm a) {
        return new AlarmResponse(
                AlarmType.COFFEE_CHAT, a.getId(),
                a.getCoffeeChat().getId(), a.getContent(),
                a.getIsRead(), a.getCreatedAt()
        );
    }
}

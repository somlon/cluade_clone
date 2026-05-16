package mju.capstone.ddingconnect.domain.job_post.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJob;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.interested_job.domain.repository.TargetJobRepository;
import mju.capstone.ddingconnect.domain.job_post.domain.GraduateJobPost;
import mju.capstone.ddingconnect.domain.job_post.domain.JobAlarm;
import mju.capstone.ddingconnect.domain.job_post.domain.JobType;
import mju.capstone.ddingconnect.domain.job_post.domain.PostContents;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.GraduateJobPostRepository;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.JobAlarmRepository;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.PostContentsRepository;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.request.UpdateJobPostRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.response.JobPostResponse;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.repository.GraduateRepository;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.JobPostHandler;
import mju.capstone.ddingconnect.global.sse.AlarmNotificationEvent;
import mju.capstone.ddingconnect.global.alarm.AlarmType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JobPostServiceImpl implements JobPostService {

    // 테스트도 동일 상수를 참조하므로 package-private 노출
    static final String JOB_ALARM_NEW_CONTENT = "관심 직무에 새로운 공고가 등록되었습니다.";
    static final String JOB_ALARM_REMOVED_CONTENT = "관심 직군에서 벗어난 공고로 변경되었습니다.";

    private final PostContentsRepository postContentsRepository;
    private final GraduateJobPostRepository graduateJobPostRepository;
    private final JobAlarmRepository jobAlarmRepository;
    private final GraduateRepository graduateRepository;
    private final TargetJobRepository targetJobRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public JobPostResponse create(Member member, CreateJobPostRequest request) {
        // 졸업생 권한 확인
        if (member.getRole() != MemberRole.GRADUATE) {
            throw new JobPostHandler(ErrorStatus.POST_CONTENTS_NOT_GRADUATE);
        }

        PostContents postContents = PostContents.builder()
                .companyImage(request.companyImage())
                .region(request.region())
                .careerType(request.careerType())
                .jobType(request.jobType())
                .country(request.country())
                .location(request.location())
                .fullLocation(request.fullLocation())
                .deadline(request.deadline())
                .detailUrl(request.detailUrl())
                .preferredLanguages(request.preferredLanguages())
                .companyName(request.companyName())
                .build();

        PostContents saved = postContentsRepository.save(postContents);

        graduateRepository.findByMemberId(member.getId()).ifPresent(graduate ->
                graduateJobPostRepository.save(GraduateJobPost.builder()
                        .graduate(graduate)
                        .postContents(saved)
                        .build())
        );

        // [알람 발행] PostContents.jobType 과 동일한 TargetJob.interestedJob 을 가진 학생에게 JobAlarm 발행.
        // JobType ↔ TargetJobCategory 는 같은 원티드 taxonomy 의 같은 11개 값으로 enum 만 분리돼 있어 name() 으로 매칭.
        // 본인(등록한 졸업생) 은 제외. 같은 멤버가 동일 카테고리를 중복 보유한 경우 1건만 발행.
        TargetJobCategory matchedCategory = toCategory(saved.getJobType());
        Long creatorId = member.getId();
        Set<Long> notifiedMemberIds = new HashSet<>();
        List<TargetJob> matched = targetJobRepository.findByInterestedJob(matchedCategory);
        for (TargetJob tj : matched) {
            Long receiverId = tj.getMember().getId();
            if (receiverId.equals(creatorId)) continue;
            if (!notifiedMemberIds.add(receiverId)) continue;
            jobAlarmRepository.save(JobAlarm.builder()
                    .member(tj.getMember())
                    .postContents(saved)
                    .content(JOB_ALARM_NEW_CONTENT)
                    .isRead(false)
                    .build());
            eventPublisher.publishEvent(new AlarmNotificationEvent(
                    tj.getMember(), AlarmType.JOB, JOB_ALARM_NEW_CONTENT));
        }

        return JobPostResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobPostResponse> getList() {
        return postContentsRepository.findAll()
                .stream().map(JobPostResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JobPostResponse getOne(Long jobPostId) {
        PostContents postContents = postContentsRepository.findById(jobPostId)
                .orElseThrow(() -> new JobPostHandler(ErrorStatus.POST_CONTENTS_NOT_FOUND));
        return JobPostResponse.from(postContents);
    }

    @Override
    @Transactional
    public JobPostResponse update(Member member, Long jobPostId, UpdateJobPostRequest request) {
        PostContents postContents = postContentsRepository.findById(jobPostId)
                .orElseThrow(() -> new JobPostHandler(ErrorStatus.POST_CONTENTS_NOT_FOUND));

        // 해당 공고를 등록한 졸업생인지 확인
        boolean isOwner = graduateJobPostRepository.findByPostContentsId(jobPostId)
                .stream()
                .anyMatch(gjp -> gjp.getGraduate().getMember().getId().equals(member.getId()));

        if (!isOwner) {
            throw new JobPostHandler(ErrorStatus.POST_CONTENTS_UNAUTHORIZED);
        }

        JobType oldJobType = postContents.getJobType();
        JobType newJobType = request.jobType() != null ? request.jobType() : postContents.getJobType();

        PostContents updated = PostContents.builder()
                .id(postContents.getId())
                .companyImage(request.companyImage() != null ? request.companyImage() : postContents.getCompanyImage())
                .region(request.region() != null ? request.region() : postContents.getRegion())
                .careerType(request.careerType() != null ? request.careerType() : postContents.getCareerType())
                .jobType(newJobType)
                .country(request.country() != null ? request.country() : postContents.getCountry())
                .location(request.location() != null ? request.location() : postContents.getLocation())
                .fullLocation(request.fullLocation() != null ? request.fullLocation() : postContents.getFullLocation())
                .deadline(request.deadline() != null ? request.deadline() : postContents.getDeadline())
                .detailUrl(request.detailUrl() != null ? request.detailUrl() : postContents.getDetailUrl())
                .preferredLanguages(request.preferredLanguages() != null ? request.preferredLanguages() : postContents.getPreferredLanguages())
                .companyName(request.companyName() != null ? request.companyName() : postContents.getCompanyName())
                .build();

        PostContents saved = postContentsRepository.save(updated);

        if (newJobType != oldJobType) {
            dispatchJobTypeChangeAlarms(saved, member.getId());
        }

        return JobPostResponse.from(saved);
    }

    /**
     * [jobType 변경 알람 디스패치]
     * prev = 이 공고에 대한 기존 JobAlarm 수신자(멤버 ID 중복 제거).
     * curr = 새 jobType 에 매칭되는 TargetJob 학생(멤버 ID 중복 제거, 등록 졸업생 본인 제외).
     *
     * Removed = prev − curr → "관심 직군에서 벗어난 공고로 변경되었습니다." 알람 추가
     * Added   = curr − prev → "관심 직무에 새로운 공고가 등록되었습니다." 알람 추가
     *
     * 기존 JobAlarm row 는 보존(삭제·갱신 X). 새 알람만 INSERT.
     */
    private void dispatchJobTypeChangeAlarms(PostContents post, Long creatorId) {
        List<JobAlarm> existing = jobAlarmRepository.findByPostContentsId(post.getId());

        Set<Long> prevIds = new HashSet<>();
        Map<Long, Member> prevMemberById = new HashMap<>();
        for (JobAlarm alarm : existing) {
            Long mid = alarm.getMember().getId();
            if (prevIds.add(mid)) {
                prevMemberById.put(mid, alarm.getMember());
            }
        }

        TargetJobCategory newCategory = toCategory(post.getJobType());
        List<TargetJob> matched = targetJobRepository.findByInterestedJob(newCategory);

        Set<Long> currIds = new HashSet<>();
        Map<Long, Member> currMemberById = new HashMap<>();
        for (TargetJob tj : matched) {
            Long mid = tj.getMember().getId();
            if (mid.equals(creatorId)) continue;
            if (currIds.add(mid)) {
                currMemberById.put(mid, tj.getMember());
            }
        }

        for (Long mid : prevIds) {
            if (currIds.contains(mid)) continue;
            if (mid.equals(creatorId)) continue;
            Member receiver = prevMemberById.get(mid);
            jobAlarmRepository.save(JobAlarm.builder()
                    .member(receiver)
                    .postContents(post)
                    .content(JOB_ALARM_REMOVED_CONTENT)
                    .isRead(false)
                    .build());
            eventPublisher.publishEvent(new AlarmNotificationEvent(
                    receiver, AlarmType.JOB, JOB_ALARM_REMOVED_CONTENT));
        }

        for (Long mid : currIds) {
            if (prevIds.contains(mid)) continue;
            Member receiver = currMemberById.get(mid);
            jobAlarmRepository.save(JobAlarm.builder()
                    .member(receiver)
                    .postContents(post)
                    .content(JOB_ALARM_NEW_CONTENT)
                    .isRead(false)
                    .build());
            eventPublisher.publishEvent(new AlarmNotificationEvent(
                    receiver, AlarmType.JOB, JOB_ALARM_NEW_CONTENT));
        }
    }

    @Override
    @Transactional
    public void delete(Member member, Long jobPostId) {
        PostContents postContents = postContentsRepository.findById(jobPostId)
                .orElseThrow(() -> new JobPostHandler(ErrorStatus.POST_CONTENTS_NOT_FOUND));

        List<GraduateJobPost> mappings = graduateJobPostRepository.findByPostContentsId(jobPostId);

        boolean isOwner = mappings.stream()
                .anyMatch(gjp -> gjp.getGraduate().getMember().getId().equals(member.getId()));

        if (!isOwner) {
            throw new JobPostHandler(ErrorStatus.POST_CONTENTS_UNAUTHORIZED);
        }

        // PostContents 를 NOT NULL FK 로 참조하는 자식 행 먼저 삭제 (TransientObjectException 방지)
        jobAlarmRepository.deleteByPostContentsId(jobPostId);
        graduateJobPostRepository.deleteAll(mappings);
        postContentsRepository.delete(postContents);
    }

    /**
     * JobType → TargetJobCategory 브리지.
     * 두 enum 은 같은 원티드 taxonomy 의 동일한 값을 공유하되 타입만 분리돼 있어 name() 으로 매칭한다.
     * 값 정렬이 어긋나면 런타임 IllegalArgumentException 이 발생한다.
     */
    private static TargetJobCategory toCategory(JobType jobType) {
        return TargetJobCategory.valueOf(jobType.name());
    }
}

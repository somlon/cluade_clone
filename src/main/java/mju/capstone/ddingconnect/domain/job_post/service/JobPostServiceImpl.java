package mju.capstone.ddingconnect.domain.job_post.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJob;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.interested_job.domain.repository.TargetJobRepository;
import mju.capstone.ddingconnect.domain.job_post.domain.GraduateJobPost;
import mju.capstone.ddingconnect.domain.job_post.domain.JobAlarm;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JobPostServiceImpl implements JobPostService {

    private final PostContentsRepository postContentsRepository;
    private final GraduateJobPostRepository graduateJobPostRepository;
    private final JobAlarmRepository jobAlarmRepository;
    private final GraduateRepository graduateRepository;
    private final TargetJobRepository targetJobRepository;

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
                .preferredLanguage(request.preferredLanguage())
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
        TargetJobCategory matchedCategory = TargetJobCategory.valueOf(saved.getJobType().name());
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
                    .content("관심 직무에 새로운 공고가 등록되었습니다.")
                    .isRead(false)
                    .build());
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

        PostContents updated = PostContents.builder()
                .id(postContents.getId())
                .companyImage(request.companyImage() != null ? request.companyImage() : postContents.getCompanyImage())
                .region(request.region() != null ? request.region() : postContents.getRegion())
                .careerType(request.careerType() != null ? request.careerType() : postContents.getCareerType())
                .jobType(request.jobType() != null ? request.jobType() : postContents.getJobType())
                .country(request.country() != null ? request.country() : postContents.getCountry())
                .location(request.location() != null ? request.location() : postContents.getLocation())
                .fullLocation(request.fullLocation() != null ? request.fullLocation() : postContents.getFullLocation())
                .deadline(request.deadline() != null ? request.deadline() : postContents.getDeadline())
                .detailUrl(request.detailUrl() != null ? request.detailUrl() : postContents.getDetailUrl())
                .preferredLanguage(request.preferredLanguage() != null ? request.preferredLanguage() : postContents.getPreferredLanguage())
                .companyName(request.companyName() != null ? request.companyName() : postContents.getCompanyName())
                .build();

        return JobPostResponse.from(postContentsRepository.save(updated));
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
}

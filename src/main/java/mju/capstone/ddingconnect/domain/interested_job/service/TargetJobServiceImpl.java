package mju.capstone.ddingconnect.domain.interested_job.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJob;
import mju.capstone.ddingconnect.domain.interested_job.domain.repository.TargetJobRepository;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.CreateTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.UpdateTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
import mju.capstone.ddingconnect.domain.job_post.domain.PostContents;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.PostContentsRepository;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.TargetJobHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TargetJobServiceImpl implements TargetJobService {

    private final TargetJobRepository targetJobRepository;
    private final PostContentsRepository postContentsRepository;

    @Override
    @Transactional
    public TargetJobResponse create(Member member, CreateTargetJobRequest request) {
        PostContents postContents = postContentsRepository.findById(request.postContentsId())
                .orElseThrow(() -> new TargetJobHandler(ErrorStatus.POST_CONTENTS_NOT_FOUND));

        TargetJob targetJob = TargetJob.builder()
                .member(member)
                .postContents(postContents)
                .interestedJob(request.interestedJob())
                .build();

        return TargetJobResponse.from(targetJobRepository.save(targetJob));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TargetJobResponse> getMyTargetJobs(Member member) {
        return targetJobRepository.findByMemberId(member.getId())
                .stream()
                .map(TargetJobResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public TargetJobResponse update(Member member, Long targetJobId, UpdateTargetJobRequest request) {
        TargetJob targetJob = targetJobRepository.findById(targetJobId)
                .orElseThrow(() -> new TargetJobHandler(ErrorStatus.TARGET_JOB_NOT_FOUND));

        // 본인 소유 확인
        if (!targetJob.getMember().getId().equals(member.getId())) {
            throw new TargetJobHandler(ErrorStatus.TARGET_JOB_UNAUTHORIZED);
        }

        TargetJob updated = TargetJob.builder()
                .id(targetJob.getId())
                .member(targetJob.getMember())
                .postContents(targetJob.getPostContents())
                .interestedJob(request.interestedJob())
                .key2(targetJob.getKey2())
                .build();

        return TargetJobResponse.from(targetJobRepository.save(updated));
    }

    @Override
    @Transactional
    public void delete(Member member, Long targetJobId) {
        TargetJob targetJob = targetJobRepository.findById(targetJobId)
                .orElseThrow(() -> new TargetJobHandler(ErrorStatus.TARGET_JOB_NOT_FOUND));

        // 본인 소유 확인
        if (!targetJob.getMember().getId().equals(member.getId())) {
            throw new TargetJobHandler(ErrorStatus.TARGET_JOB_UNAUTHORIZED);
        }

        targetJobRepository.delete(targetJob);
    }
}

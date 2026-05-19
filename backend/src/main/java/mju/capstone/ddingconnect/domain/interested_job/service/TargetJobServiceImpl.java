package mju.capstone.ddingconnect.domain.interested_job.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJob;
import mju.capstone.ddingconnect.domain.interested_job.domain.repository.TargetJobRepository;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.ReplaceTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
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

    /**
     * 본인 관심 직군 전체를 요청 리스트로 교체한다.
     * deleteByMemberId 후 입력 리스트(중복 제거)만큼 save 하는 단일 트랜잭션.
     */
    @Override
    @Transactional
    public List<TargetJobResponse> replace(Member member, ReplaceTargetJobRequest request) {
        if (request.categories() == null) {
            throw new TargetJobHandler(ErrorStatus._BAD_REQUEST);
        }

        targetJobRepository.deleteByMemberId(member.getId());

        return request.categories().stream()
                .distinct()
                .map(category -> targetJobRepository.save(TargetJob.builder()
                        .member(member)
                        .interestedJob(category)
                        .build()))
                .map(TargetJobResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TargetJobResponse> getMyTargetJobs(Member member) {
        return targetJobRepository.findByMemberId(member.getId())
                .stream()
                .map(TargetJobResponse::from)
                .toList();
    }
}

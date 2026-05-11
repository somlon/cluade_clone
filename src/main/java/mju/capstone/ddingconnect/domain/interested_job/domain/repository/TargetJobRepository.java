package mju.capstone.ddingconnect.domain.interested_job.domain.repository;

import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJob;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * [관심 직군 레포지토리]
 * TargetJob 엔티티에 대한 데이터베이스 접근 인터페이스
 */
public interface TargetJobRepository extends JpaRepository<TargetJob, Long> {

    List<TargetJob> findByMemberId(Long memberId);

    List<TargetJob> findByMemberIdAndInterestedJob(Long memberId, TargetJobCategory interestedJob);

    List<TargetJob> findByInterestedJob(TargetJobCategory interestedJob);
}

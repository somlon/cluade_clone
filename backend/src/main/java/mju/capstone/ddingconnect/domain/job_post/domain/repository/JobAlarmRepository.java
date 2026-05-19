package mju.capstone.ddingconnect.domain.job_post.domain.repository;

import mju.capstone.ddingconnect.domain.job_post.domain.JobAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * [구직 알람 레포지토리]
 * JobAlarm 엔티티에 대한 데이터베이스 접근 인터페이스
 */
public interface JobAlarmRepository extends JpaRepository<JobAlarm, Long> {

    List<JobAlarm> findByMemberId(Long memberId);

    List<JobAlarm> findByMemberIdAndIsRead(Long memberId, Boolean isRead);

    long countByMemberIdAndIsRead(Long memberId, Boolean isRead);

    List<JobAlarm> findByPostContentsId(Long postContentsId);

    void deleteByPostContentsId(Long postContentsId);

    void deleteByMemberId(Long memberId);
}

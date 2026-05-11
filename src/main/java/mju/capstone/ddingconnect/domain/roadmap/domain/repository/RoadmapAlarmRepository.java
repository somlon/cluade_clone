package mju.capstone.ddingconnect.domain.roadmap.domain.repository;

import mju.capstone.ddingconnect.domain.roadmap.domain.RoadmapAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * [로드맵 알람 레포지토리]
 * RoadmapAlarm 엔티티에 대한 데이터베이스 접근 인터페이스
 */
public interface RoadmapAlarmRepository extends JpaRepository<RoadmapAlarm, Long> {

    List<RoadmapAlarm> findByRoadmapId(Long roadmapId);

    List<RoadmapAlarm> findByRoadmapIdAndIsRead(Long roadmapId, Boolean isRead);

    long countByRoadmapIdAndIsRead(Long roadmapId, Boolean isRead);

    /**
     * 특정 회원의 로드맵에 연관된 알람을 조회
     */
    @Query("SELECT ra FROM RoadmapAlarm ra WHERE ra.roadmap.member.id = :memberId")
    List<RoadmapAlarm> findByRoadmapOwnerId(@Param("memberId") Long memberId);
}

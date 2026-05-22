package mju.capstone.ddingconnect.domain.roadmap.domain.repository;

import mju.capstone.ddingconnect.domain.roadmap.domain.Roadmap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * [로드맵 레포지토리]
 * Roadmap 엔티티에 대한 데이터베이스 접근 인터페이스
 */
public interface RoadmapRepository extends JpaRepository<Roadmap, Long> {

    List<Roadmap> findByMemberId(Long memberId);

    /** 회원이 생성한 로드맵을 최신순(createdAt 내림차순)으로 조회 — 마이 로드맵 목록 화면용 */
    List<Roadmap> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    long countByMemberId(Long memberId);
}

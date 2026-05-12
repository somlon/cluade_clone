package mju.capstone.ddingconnect.domain.member.domain.repository;

import mju.capstone.ddingconnect.domain.member.domain.Graduate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * [졸업생 레포지토리]
 * Graduate 엔티티에 대한 데이터베이스 접근 인터페이스
 */
public interface GraduateRepository extends JpaRepository<Graduate, Long> {

    Optional<Graduate> findByMemberId(Long memberId);

    boolean existsByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}

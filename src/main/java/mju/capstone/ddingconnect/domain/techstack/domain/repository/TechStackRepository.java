package mju.capstone.ddingconnect.domain.techstack.domain.repository;

import mju.capstone.ddingconnect.domain.techstack.domain.TechStack;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * [기술 스택 레포지토리]
 * TechStack 엔티티에 대한 데이터베이스 접근 인터페이스
 */
public interface TechStackRepository extends JpaRepository<TechStack, Long> {

    List<TechStack> findByMemberId(Long memberId);

    boolean existsByMemberIdAndName(Long memberId, TechStackName name);

    void deleteByMemberId(Long memberId);
}

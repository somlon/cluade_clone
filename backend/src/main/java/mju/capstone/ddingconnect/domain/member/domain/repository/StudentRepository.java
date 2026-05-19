package mju.capstone.ddingconnect.domain.member.domain.repository;

import mju.capstone.ddingconnect.domain.member.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * [재학생 레포지토리]
 * Student 엔티티에 대한 데이터베이스 접근 인터페이스
 */
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByMemberId(Long memberId);

    boolean existsByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}

package mju.capstone.ddingconnect.domain.job_post.domain.repository;

import mju.capstone.ddingconnect.domain.job_post.domain.GraduateJobPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * [졸업생이 생성한 구직공고 레포지토리]
 * GraduateJobPost 엔티티에 대한 데이터베이스 접근 인터페이스
 */
public interface GraduateJobPostRepository extends JpaRepository<GraduateJobPost, Long> {

    List<GraduateJobPost> findByGraduateId(Long graduateId);

    List<GraduateJobPost> findByPostContentsId(Long postContentsId);

    boolean existsByGraduateIdAndPostContentsId(Long graduateId, Long postContentsId);
}

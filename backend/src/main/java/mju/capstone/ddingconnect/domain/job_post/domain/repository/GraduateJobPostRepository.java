package mju.capstone.ddingconnect.domain.job_post.domain.repository;

import mju.capstone.ddingconnect.domain.job_post.domain.GraduateJobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * [졸업생이 생성한 구직공고 레포지토리]
 * GraduateJobPost 엔티티에 대한 데이터베이스 접근 인터페이스
 */
public interface GraduateJobPostRepository extends JpaRepository<GraduateJobPost, Long> {

    List<GraduateJobPost> findByGraduateId(Long graduateId);

    List<GraduateJobPost> findByPostContentsId(Long postContentsId);

    boolean existsByGraduateIdAndPostContentsId(Long graduateId, Long postContentsId);

    /**
     * 선배가 등록한 공고의 PostContents id 목록(중복 제거).
     * 구직 정보 화면 분리 조회에서 사용 — 선배 공고(매핑 존재) vs 일반 공고(매핑 없음) 구분 기준.
     */
    @Query("SELECT DISTINCT g.postContents.id FROM GraduateJobPost g")
    List<Long> findDistinctPostContentsIds();
}

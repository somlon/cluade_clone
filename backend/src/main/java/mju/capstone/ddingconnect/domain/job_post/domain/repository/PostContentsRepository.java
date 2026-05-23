package mju.capstone.ddingconnect.domain.job_post.domain.repository;

import mju.capstone.ddingconnect.domain.job_post.domain.CareerType;
import mju.capstone.ddingconnect.domain.job_post.domain.JobType;
import mju.capstone.ddingconnect.domain.job_post.domain.PostContents;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * [구직 공고 레포지토리]
 * PostContents 엔티티에 대한 데이터베이스 접근 인터페이스
 */
public interface PostContentsRepository extends JpaRepository<PostContents, Long> {

    List<PostContents> findByJobType(JobType jobType);

    List<PostContents> findByCareerType(CareerType careerType);

    List<PostContents> findByCompanyName(String companyName);

    List<PostContents> findByIdIn(Collection<Long> ids);

    List<PostContents> findByIdNotIn(Collection<Long> ids);
}

package mju.capstone.ddingconnect.domain.job_post.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.global.common.BaseEntity;

import java.time.LocalDate;

/**
 * [구직 공고 엔티티]
 * ERD 컬럼 매핑:
 * - PK(Long)              → id
 * - 회사이미지              → companyImage (varchar(255))
 * - 위치                   → region (varchar(255))
 * - 경력(ENUM)             → careerType (CareerType)
 * - 직무(ENUM)             → jobType (JobType)
 * - 주소1                  → country (varchar(255))
 * - 주소2                  → location (varchar(255))
 * - 주소3                  → fullLocation (varchar(255), column: full_location)
 * - 마감일(Date)           → deadline
 * - 상세URL                → detailUrl (varchar(255))
 * - 선호언어               → preferredLanguage (varchar(255))
 * - 회사명                 → companyName (varchar(255))
 *
 * 연결 관계:
 * - 졸업생이 생성한 구직공고(GraduateJobPost): 1:N (GraduateJobPost.PK3 → 구직공고.PK)
 * - 구직알람(JobAlarm): 1:N (JobAlarm.PK3 → 구직공고.PK)
 * - 관심직군(TargetJob): 1:N (TargetJob.FK2 → 구직공고.PK)
 */
@Entity
@Table(name = "post_contents")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostContents extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String companyImage;      // 회사이미지

    @Column(length = 255)
    private String region;            // 위치

    @Enumerated(EnumType.STRING)
    private CareerType careerType;    // 경력(ENUM)

    @Enumerated(EnumType.STRING)
    private JobType jobType;          // 직무(ENUM)

    @Column(length = 255)
    private String country;           // 주소1

    @Column(length = 255)
    private String location;          // 주소2

    @Column(name = "full_location", length = 255)
    private String fullLocation;      // 주소3

    private LocalDate deadline;       // 마감일

    @Column(length = 255)
    private String detailUrl;         // 상세URL

    @Column(length = 255)
    private String preferredLanguage; // 선호언어

    @Column(length = 255)
    private String companyName;       // 회사명
}

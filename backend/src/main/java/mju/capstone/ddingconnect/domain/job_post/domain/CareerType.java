package mju.capstone.ddingconnect.domain.job_post.domain;

/**
 * [경력 유형 ENUM]
 * ERD의 '구직 공고' 테이블의 '경력(ENUM)' 컬럼
 */
public enum CareerType {
    NEW_GRADUATE, // 신입
    JUNIOR,       // 주니어 (1~3년)
    SENIOR,       // 시니어 (4~7년)
    LEAD,         // 리드 (8년 이상)
    ANY           // 경력 무관
}

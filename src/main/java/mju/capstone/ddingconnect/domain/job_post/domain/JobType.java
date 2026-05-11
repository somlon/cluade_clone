package mju.capstone.ddingconnect.domain.job_post.domain;

/**
 * [직무 유형 ENUM]
 * ERD의 '구직 공고' 테이블의 '직무(ENUM)' 컬럼
 */
public enum JobType {
    BACKEND,   // 백엔드
    FRONTEND,  // 프론트엔드
    FULLSTACK, // 풀스택
    MOBILE,    // 모바일
    AI_ML,     // AI/머신러닝
    DATA,      // 데이터
    DEVOPS,    // DevOps/인프라
    SECURITY,  // 보안
    GAME,      // 게임
    EMBEDDED,  // 임베디드
    ETC        // 기타
}

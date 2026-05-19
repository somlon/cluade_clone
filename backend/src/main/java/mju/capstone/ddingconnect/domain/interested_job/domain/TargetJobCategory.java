package mju.capstone.ddingconnect.domain.interested_job.domain;

/**
 * [관심 직군 카테고리 ENUM]
 * ERD의 '관심 직군' 테이블의 'Interested_Job(ENUM)' 컬럼
 */
public enum TargetJobCategory {
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

package mju.capstone.ddingconnect.domain.member.dto.request;

public record UpdateMemberRequest(

        // ── 공통 필드 (STUDENT / GRADUATE 모두 수정 가능) ──────────
        String nickname,
        String studentNumber,
        String department,
        String githubLink,
        String linkedinLink,
        String portfolio,
        String profileImage,

        // ── STUDENT 전용 필드 ──────────────────────────────────────
        Integer grade,          // 학년

        // ── GRADUATE 전용 필드 ────────────────────────────────────
        String businessCardImage,   // 명함이미지
        String company,             // 회사명
        Integer careerYear          // 경력
) {}

package mju.capstone.ddingconnect.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.global.common.BaseEntity;

import static mju.capstone.ddingconnect.domain.member.domain.MemberRole.UNKNOWN;

/**
 * [회원 엔티티]
 * ERD 컬럼 매핑:
 * - PK(Long)       → id
 * - 명지대 이메일   → email (varchar(255))
 * - 닉네임         → nickname (varchar(255))
 * - 비밀번호        → password (varchar(255))
 * - 학번           → studentNumber (varchar(255))
 * - 학과           → department (varchar(255))
 * - 깃허브 링크     → githubLink (varchar(255))
 * - 링크드인 링크   → linkedinLink (varchar(255))
 * - 포트폴리오      → portfolio (varchar(255))
 * - 프로필이미지    → profileImage (varchar(255))
 * - 포인트         → point (Long)
 * - 인증서         → certificate (varchar(255))
 * - 회원 탈퇴 여부  → isDeleted (boolean)
 *
 * 연결 관계:
 * - 재학생(Student): 1:1 (Student.FK → 회원.PK)
 * - 졸업생(Graduate): 1:1 (Graduate.FK → 회원.PK)
 * - 기술스택(TechStack): 1:N (TechStack.FK → 회원.PK)
 * - 질문(Question): 1:N (Question.FK → 회원.PK)
 * - 질문좋아요(QuestionLike): 1:N (QuestionLike.FK → 회원.PK)
 * - 답변(Answer): 1:N (Answer.PK2 → 회원.PK)
 * - 커피챗(CoffeeChat): 1:N (요청자/수신자 양방향)
 * - 답변알람(AnswerAlarm): 1:N (AnswerAlarm.FK → 회원.PK)
 * - 관심직군(InterestedJob): 1:N (InterestedJob.FK → 회원.PK)
 * - 구직알람(JobAlarm): 1:N (JobAlarm.PK2 → 회원.PK)
 * - 로드맵알람(RoadmapAlarm): 1:N (RoadmapAlarm.PK2 → 회원.PK)
 */
@Entity
@Table(name = "member")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String email;           // 명지대 이메일

    @Column(length = 255)
    private String nickname;        // 닉네임

    @Column(length = 255)
    private String password;        // 비밀번호

    @Column(length = 255)
    private String studentNumber;   // 학번

    @Column(length = 255)
    private String department;      // 학과

    @Column(length = 255)
    private String githubLink;      // 깃허브 링크

    @Column(length = 255)
    private String linkedinLink;    // 링크드인 링크

    @Column(length = 255)
    private String portfolio;       // 포트폴리오

    @Column(length = 255)
    private String profileImage;    // 프로필이미지

    private Long point;             // 포인트

    @Column(length = 255)
    private String certificate;     // 인증서

    private Boolean isDeleted;      // 회원 탈퇴 여부

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private MemberRole role = UNKNOWN; // 회원 역할 (UNKNOWN/STUDENT/GRADUATE)
}

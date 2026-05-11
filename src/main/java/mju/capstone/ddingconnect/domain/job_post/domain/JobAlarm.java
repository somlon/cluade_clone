package mju.capstone.ddingconnect.domain.job_post.domain;

import jakarta.persistence.*;
import lombok.*;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.common.BaseEntity;

/**
 * [구직 알람 엔티티]
 * ERD 컬럼 매핑 (ERD에 명시된 컬럼 그대로):
 * - PK(Long)           → id
 * - PK2(Long)          → member (회원.PK 참조, 알람 수신자)
 * - PK3(Long)          → postContents (구직공고.PK 참조)
 * - 알람 내용           → content (varchar(255))
 * - 읽음 여부(Boolean)  → isRead
 *
 * 연결 관계:
 * - 회원(Member): N:1 (구직알람.PK2 → 회원.PK)
 * - 구직공고(PostContents): N:1 (구직알람.PK3 → 구직공고.PK)
 */
@Entity
@Table(name = "job_alarm")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobAlarm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;              // ERD의 PK2 → 회원.PK (알람 수신자)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_post_id", nullable = false)
    private PostContents postContents;  // ERD의 PK3 → 구직공고.PK

    @Column(length = 255)
    private String content;             // 알람 내용

    private Boolean isRead;             // 읽음 여부
}

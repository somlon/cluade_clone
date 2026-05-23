package mju.capstone.ddingconnect.domain.coffeechat.dto.response;

import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatStatus;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;

import java.util.List;

/**
 * [나의 활동/커피챗 카드 응답 DTO]
 *
 * 마이페이지 '커피챗 수' 카드를 클릭해 진입하는 '나의 활동/커피챗' 화면에서
 * 한 카드당 표시되는 정보. 본인 정보 대신 <b>상대방(파트너)</b> 정보를 노출한다
 * (본인이 requester 면 receiver, receiver 면 requester).
 *
 * - coffeeChatId: 커피챗 row PK
 * - status: 현재 상태(PENDING/ACCEPTED/REJECTED) — 화면 필터/뱃지 용
 * - partnerId/nickname/department: 상대방 식별 + 카드 헤더
 * - partnerJobs: 상대방 관심 직군(`TargetJob.interestedJob` 리스트) — 카드 본문
 * - partnerTechStacks: 상대방 기술 스택 리스트 — 카드 본문 칩 표시
 */
public record CoffeeChatPartnerResponse(
        Long coffeeChatId,
        CoffeeChatStatus status,
        Long partnerId,
        String partnerNickname,
        String partnerDepartment,
        List<TargetJobCategory> partnerJobs,
        List<TechStackName> partnerTechStacks
) {}

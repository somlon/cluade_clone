package mju.capstone.ddingconnect.domain.coffeechat.dto.request;

import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatStatus;

/**
 * [커피챗 상태 변경 요청 DTO]
 * @param status 변경할 상태 (ACCEPTED / REJECTED)
 */
public record UpdateCoffeeChatStatusRequest(
        CoffeeChatStatus status
) {}

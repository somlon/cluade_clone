package mju.capstone.ddingconnect.domain.coffeechat.dto.request;

/**
 * [커피챗 요청 DTO]
 * @param receiverId 수신자 회원 PK
 * @param kakaoOpenChatLink 카카오 오픈채팅 링크
 */
public record CreateCoffeeChatRequest(
        Long receiverId,
        String kakaoOpenChatLink
) {}

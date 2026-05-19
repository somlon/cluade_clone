package mju.capstone.ddingconnect.domain.coffeechat.dto.response;

import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChat;
import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatStatus;

/**
 * [커피챗 응답 DTO]
 */
public record CoffeeChatResponse(
        Long id,
        Long requesterId,
        Long receiverId,
        CoffeeChatStatus status,
        String kakaoOpenChatLink
) {
    public static CoffeeChatResponse from(CoffeeChat coffeeChat) {
        return new CoffeeChatResponse(
                coffeeChat.getId(),
                coffeeChat.getRequester().getId(),
                coffeeChat.getReceiver().getId(),
                coffeeChat.getStatus(),
                coffeeChat.getKakaoOpenChatLink()
        );
    }
}

package mju.capstone.ddingconnect.domain.coffeechat.domain.repository;

import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChat;
import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * [커피챗 레포지토리]
 * CoffeeChat 엔티티에 대한 데이터베이스 접근 인터페이스
 */
public interface CoffeeChatRepository extends JpaRepository<CoffeeChat, Long> {

    List<CoffeeChat> findByRequesterId(Long requesterId);

    List<CoffeeChat> findByReceiverId(Long receiverId);

    List<CoffeeChat> findByReceiverIdAndStatus(Long receiverId, CoffeeChatStatus status);
}

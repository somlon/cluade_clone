package mju.capstone.ddingconnect.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * [JPA Auditing 설정]
 * BaseEntity(공통 정보)의 createdAt, updatedAt 자동 주입을 위한 설정 클래스
 * @SpringBootApplication과 분리하여 @DataJpaTest 환경에서 @Import로도 활성화 가능
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}

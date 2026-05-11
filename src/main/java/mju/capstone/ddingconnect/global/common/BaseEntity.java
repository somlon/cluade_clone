package mju.capstone.ddingconnect.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * [공통 정보 (Audit Base Entity)]
 * ERD의 '공통 정보' 테이블에 해당하는 추상 기반 클래스
 * ERD에 명시된 Field(Datetime) 컬럼을 createdAt, updatedAt 두 컬럼으로 구현
 * @MappedSuperclass: 실제 DB 테이블은 생성되지 않으며, 이를 상속하는 모든 엔티티 테이블에 포함
 *
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}

package org.demo.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
public class UniOtoPostDetail {
    // 부모와 독립적인 고유의 PK를 가짐
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    String createdBy;
    LocalDateTime createdOn;
    // CascadeType.ALL이 빠지면 예외 발생함
    // Caused by: org.hibernate.TransientPropertyValueException: object references an unsaved transient instance
    // - save the transient instance before flushing : org.demo.jpa.entity.UniOtoPostDetail.post -> org.demo.jpa.entity.UniOtoPost
    @OneToOne(cascade = CascadeType.ALL)
    // @JoinColumn을 명시하지 않아도 FK 컬럼이 생성됨. 있으면 명시적이어서 좋음
    @JoinColumn(name = "post_id")
    private UniOtoPost post;
}

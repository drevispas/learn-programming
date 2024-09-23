package org.demo.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
public class UniOtoNoFkPostDetail {
    // @MapId를 사용하면 실제로는 "id" 컬럼을 사용하지 않음
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    String createdBy;
    LocalDateTime createdOn;
    // CascadeType.ALL이 빠지면 예외 발생함
    // Caused by: org.hibernate.TransientPropertyValueException: object references an unsaved transient instance
    // - save the transient instance before flushing : org.demo.jpa.entity.UniOtoPostDetail.post -> org.demo.jpa.entity.UniOtoPost
    @OneToOne(cascade = CascadeType.ALL)
    // @MapsId를 사용하면 부모의 PK를 자식의 PK로 사용함
    @MapsId
    private UniOtoPost post;
}

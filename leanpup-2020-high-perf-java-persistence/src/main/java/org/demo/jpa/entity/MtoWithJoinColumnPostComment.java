package org.demo.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class MtoWithJoinColumnPostComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    String review;
    // CascadeType.ALL 옵션 없으면 아래와 같은 에러 발생
    // https://www.baeldung.com/hibernate-unsaved-transient-instance-error
    // Caused by: org.hibernate.TransientPropertyValueException: object references an unsaved transient instance
    // - save the transient instance before flushing : org.demo.jpa.entity.MTOPostComment.post -> org.demo.jpa.entity.MTOPost
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "post_id")
    private MtoPost post;
}

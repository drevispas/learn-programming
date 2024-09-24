package org.demo.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class BiOtmPostComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    String review;
    @ManyToOne(cascade = CascadeType.ALL)
    // @JoinColumn을 명시하지 않아도 FK 컬럼이 생성됨. 있으면 명시적이어서 좋음
    @JoinColumn(name = "post_id")
    private BiOtmPost post;
}

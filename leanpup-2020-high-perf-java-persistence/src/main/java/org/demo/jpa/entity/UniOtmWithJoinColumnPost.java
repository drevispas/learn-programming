package org.demo.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class UniOtmWithJoinColumnPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    String title;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    // JoinColumn is used to specify the foreign key column in the UniOtmPostComment table
    // By JoinColumn, any junction table is not created
    @JoinColumn(name = "post_id")
    private List<UniOtmPostComment> comments = new ArrayList<>();
}

package org.demo.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class UniOtmPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    String title;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UniOtmPostComment> comments = new ArrayList<>();
}

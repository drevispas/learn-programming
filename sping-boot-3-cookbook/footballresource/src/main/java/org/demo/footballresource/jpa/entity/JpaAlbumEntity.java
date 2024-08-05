package org.demo.footballresource.jpa.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;

// Album entity contains a list of cards
@Table(name = "albums")
@Entity
public class JpaAlbumEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;
    private LocalDate expireDate;
    @OneToMany
    private List<JpaCardEntity> cards;
}

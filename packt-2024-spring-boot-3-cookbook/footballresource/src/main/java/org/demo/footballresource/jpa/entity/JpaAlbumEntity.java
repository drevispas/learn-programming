package org.demo.footballresource.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
// Album entity contains a list of cards
@Table(name = "albums")
@Entity
public class JpaAlbumEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;
    private LocalDate expireDate;
    // @OneToMany uses LAZY by default
    @OneToMany
    private List<JpaCardEntity> cards;
    // @ManyToOne uses EAGER by default
    @ManyToOne
    // @JoinColumn specifies the foreign key column in the albums table and is often used with @ManyToOne
    // This table only has one foreign key column, owner_id, which references the id column of the users table
    @JoinColumn(name = "owner_id", nullable = false)
    private JpaUserEntity owner;
}

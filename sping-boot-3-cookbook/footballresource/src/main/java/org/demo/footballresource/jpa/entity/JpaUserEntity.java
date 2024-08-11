package org.demo.footballresource.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
@Table(name = "users")
@Entity
public class JpaUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String username;
    @OneToMany(mappedBy = "owner")
    private List<JpaCardEntity> ownedCards;
    @OneToMany(mappedBy = "owner")
    private Set<JpaAlbumEntity> ownedAlbums;

    public JpaUserEntity(String name) {
        this.username = name;
    }
}

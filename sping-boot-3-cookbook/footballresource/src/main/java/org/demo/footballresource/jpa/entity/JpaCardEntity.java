package org.demo.footballresource.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Card entity contains a detail of a player
@Table(name = "cards", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"album_id", "player_id"})
})
@Entity
public class JpaCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    // If it is null, the card is not in any album
    @JoinColumn(name="album_id")
    private JpaAlbumEntity album;
    @ManyToOne
    @JoinColumn(name="player_id", nullable = false)
    private JpaPlayerEntity player;
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private JpaUserEntity owner;
}

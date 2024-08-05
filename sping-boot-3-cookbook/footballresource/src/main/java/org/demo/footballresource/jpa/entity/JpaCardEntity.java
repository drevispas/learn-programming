package org.demo.footballresource.jpa.entity;

import jakarta.persistence.*;

// Card entity contains a detail of a player
@Table(name = "cards")
@Entity
public class JpaCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name="album_id", nullable = false)
    private JpaAlbumEntity album;
    @ManyToOne
    @JoinColumn(name="player_id", nullable = false)
    private JpaPlayerEntity player;
}

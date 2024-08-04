package org.demo.footballresource.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Table(name = "players")
@Entity
public class JpaPlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer jerseyNumber;
    private String name;
    private String position;
    private LocalDate dateOfBirth;
    // ManyToOne uses EAGER by default
    @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn specifies the foreign key column in the players table and is often used with @ManyToOne
    @JoinColumn(name = "team_id")
    private JpaTeamEntity team;
}

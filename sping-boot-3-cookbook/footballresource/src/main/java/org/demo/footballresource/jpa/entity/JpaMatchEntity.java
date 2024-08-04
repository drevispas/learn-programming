package org.demo.footballresource.jpa.entity;


import jakarta.persistence.*;

import java.time.LocalDate;

@Table(name = "matches")
@Entity
public class JpaMatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private LocalDate matchDate;
    @ManyToOne
    @JoinColumn(name="team1_id", nullable = false)
    private JpaTeamEntity team1;
    @ManyToOne
    // team2_id column of matches table is the foreign key that references the id column of the teams table
    @JoinColumn(name="team2_id", nullable = false)
    private JpaTeamEntity team2;
    @Column(name="team1_goals", columnDefinition = "integer default 0")
    private Integer team1Goals;
    @Column(name="team2_goals", columnDefinition = "integer default 0")
    private Integer team2Goals;
}

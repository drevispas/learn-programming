package org.demo.footballresource.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "teams")
@Entity
public class JpaTeamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    // Hibernate will retrieve players when the players field in teams entity is accessed
    // The other way, EAGER, would retrieve the players at the same time the parent team is retrieved
    // OneToMany uses EAGER by default
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "team")
    private List<JpaPlayerEntity> players;
}

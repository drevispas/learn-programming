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
    private Integer id;
    private String name;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "team")
    private List<JpaPlayerEntity> players;
}

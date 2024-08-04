package org.demo.footballresource.jpa.repository;

import org.demo.footballresource.jpa.entity.JpaTeamEntity;
import org.springframework.data.repository.CrudRepository;

public interface JpaTeamRepository extends CrudRepository<JpaTeamEntity, Integer> {
}

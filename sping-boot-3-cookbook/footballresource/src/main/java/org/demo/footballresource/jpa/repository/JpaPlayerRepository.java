package org.demo.footballresource.jpa.repository;

import org.demo.footballresource.jpa.entity.JpaPlayerEntity;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface JpaPlayerRepository extends CrudRepository<JpaPlayerEntity, Integer> {

    List<JpaPlayerEntity> findByDateOfBirth(LocalDate dateOfBirth);
    List<JpaPlayerEntity> findByNameContaining(String name);
}

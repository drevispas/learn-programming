package org.demo.todo.repository;

import jakarta.validation.constraints.NotBlank;
import org.demo.todo.model.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findByUserId(@NotBlank String userId);
}

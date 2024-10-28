package org.demo.backend.service;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.backend.exception.TodoException;
import org.demo.backend.model.Todo;
import org.demo.backend.repository.TodoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class TodoService {

    private final TodoRepository todoRepository;

    @Transactional(readOnly = true)
    public Todo getTodoById(Long id) {
        return todoRepository.findById(id).orElseThrow(
                () -> {
                    log.error("Todo not found for id: {}", id);
                    return new TodoException.TodoNotFoundException("Todo not found for id: " + id);
                }
        );
    }

    @Transactional(readOnly = true)
    public List<Todo> getTodos(String userId) {
        return todoRepository.findByUserId(userId);
    }

    @Transactional
    public Todo create(@NotNull Todo todo) {
        Todo savedTodo = todoRepository.save(todo);
        log.info("Todo created: {}", savedTodo);
        return savedTodo;
    }

    @Transactional
    public Todo update(Todo todo) {
        Todo existingTodo = getTodoById(todo.getId());
        Todo updatingTodo = todo.update(existingTodo);
        Todo updatedTodo = todoRepository.save(updatingTodo);
        log.info("Todo updated: {}", updatedTodo);
        return updatedTodo;
    }

    @Transactional
    public void delete(Long id) {
        todoRepository.deleteById(id);
    }
}

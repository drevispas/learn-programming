package org.demo.todo.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.todo.model.Todo;
import org.demo.todo.service.TodoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/todos")
@RestController
public class TodoController {

    private static final String TEMP_USER_ID = "user_id";

    private final TodoService todoService;


    public record TodoCreateRequest(@NotBlank String title, String description) {

        public static Todo toModel(TodoCreateRequest request) {
            return Todo.builder()
                    .userId(TodoController.TEMP_USER_ID)
                    .title(request.title())
                    .description(request.description())
                    .completed(false)
                    .build();
        }
    }

    public record TodoUpdateRequest(Long id, String title, String description, Boolean completed) {

        public static Todo toModel(Long id, TodoUpdateRequest request) {
            return Todo.builder()
                    .id(id)
                    .userId(TEMP_USER_ID)
                    .title(request.title())
                    .description(request.description())
                    .completed(request.completed())
                    .build();
        }
    }

    public record TodoResponseData(Long id, String title, String description, Boolean completed) {
        public TodoResponseData(Todo todo) {
            this(todo.getId(), todo.getTitle(), todo.getDescription(), todo.getCompleted());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TodoResponseData>> getTodoById(@PathVariable Long id) {
        return ApiResponse.ok(new TodoResponseData(todoService.getTodoById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TodoResponseData>>> getTodos() {
        return ApiResponse.ok(todoService.getTodos(TEMP_USER_ID).stream().map(TodoResponseData::new).collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TodoResponseData>> create(@RequestBody TodoCreateRequest request) {
        return ApiResponse.created(new TodoResponseData(todoService.create(TodoCreateRequest.toModel(request))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TodoResponseData>> update(@PathVariable Long id, @RequestBody TodoUpdateRequest request) {
        return ApiResponse.ok(new TodoResponseData(todoService.update(TodoUpdateRequest.toModel(id, request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        todoService.delete(id);
        return ApiResponse.noContent();
    }
}

package org.demo.backend.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.demo.backend.model.Todo;
import org.demo.backend.service.TodoService;
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

    @GetMapping("/{id}")
    public TodoResponse<TodoResponseData> getTodoById(@PathVariable Long id) {
        return TodoResponse.<TodoResponseData>builder()
                .data(new TodoResponseData(todoService.getTodoById(id)))
                .build();
    }

    @GetMapping
    public TodoResponse<List<TodoResponseData>> getTodos(@RequestParam String userId) {
        return TodoResponse.<List<TodoResponseData>>builder()
                .data(todoService.getTodos(userId).stream().map(TodoResponseData::new).collect(Collectors.toList()))
                .build();

    }

    @PostMapping
    public TodoResponse<TodoResponseData> create(@RequestBody TodoCreateRequest request) {
        return TodoResponse.<TodoResponseData>builder()
                .data(new TodoResponseData(todoService.create(TodoCreateRequest.toModel(request))))
                .build();
    }

    @PutMapping
    public TodoResponse<TodoResponseData> update(@RequestBody TodoUpdateRequest request) {
        return TodoResponse.<TodoResponseData>builder()
                .data(new TodoResponseData(todoService.update(TodoUpdateRequest.toModel(request))))
                .build();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        todoService.delete(id);
    }

    @Getter
    @Setter
    public static class TodoCreateRequest {
        @NotBlank
        private String title;
        private String description;

        public static Todo toModel(TodoCreateRequest request) {
            return Todo.builder()
                    .userId(TEMP_USER_ID)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .completed(false)
                    .build();
        }
    }

    @Getter
    @Setter
    public static class TodoUpdateRequest {
        private Long id;
        private String title;
        private String description;
        private Boolean completed;

        public static Todo toModel(TodoUpdateRequest request) {
            return Todo.builder()
                    .id(request.getId())
                    .userId(TEMP_USER_ID)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .completed(request.completed)
                    .build();
        }
    }

    @Getter
    public static class TodoResponseData {
        private Long id;
        private String title;
        private String description;
        private Boolean completed;

        public TodoResponseData(Todo todo) {
            this.id = todo.getId();
            this.title = todo.getTitle();
            this.description = todo.getDescription();
            this.completed = todo.getCompleted();
        }
    }
}

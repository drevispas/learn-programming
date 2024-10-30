package org.demo.todo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.demo.todo.entity.BaseEntity;

@ToString(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class Todo extends BaseEntity {

    @NotBlank
    @Column(name = "user_id", nullable = false)
    private String userId;
    @NotBlank
    @Column(name = "title", nullable = false)
    private String title;
    private String description;
    @Column(name = "completed", nullable = false, columnDefinition = "boolean default false")
    private Boolean completed;

    public Todo update(Todo existingTodo) {
        if (this.title != null) {
            existingTodo.setTitle(this.title);
        }
        if (this.description != null) {
            existingTodo.setDescription(this.description);
        }
        if (this.completed != null) {
            existingTodo.setCompleted(this.completed);
        }
        return existingTodo;
    }
}

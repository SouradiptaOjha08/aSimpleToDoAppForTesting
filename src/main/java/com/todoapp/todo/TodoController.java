package com.todoapp.todo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/todos")
class TodoController {
    private final TodoService service;

    TodoController(TodoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TodoResponse create(@Valid @RequestBody CreateTodoRequest request) {
        return TodoResponse.from(service.create(request.title()));
    }

    @GetMapping
    List<TodoResponse> list() {
        return service.list().stream().map(TodoResponse::from).toList();
    }

    @PatchMapping("/{id}/completed")
    TodoResponse complete(@PathVariable long id) {
        return TodoResponse.from(service.complete(id));
    }

    record CreateTodoRequest(@NotBlank @Size(max = 200) String title) {
    }

    record TodoResponse(Long id, String title, boolean completed) {
        static TodoResponse from(Todo todo) {
            return new TodoResponse(todo.getId(), todo.getTitle(), todo.isCompleted());
        }
    }
}

package com.todoapp.todo;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
class TodoService {
    private final TodoRepository repository;

    TodoService(TodoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    Todo create(String title) {
        return repository.save(new Todo(title.trim()));
    }

    @Transactional(readOnly = true)
    List<Todo> list() {
        return repository.findAllByOrderByIdAsc();
    }

    @Transactional
    Todo complete(long id) {
        Todo todo = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found"));
        todo.markCompleted();
        return todo;
    }
}

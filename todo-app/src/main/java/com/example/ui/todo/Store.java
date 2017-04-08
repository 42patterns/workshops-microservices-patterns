package com.example.ui.todo;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Named;
import javax.interceptor.Interceptors;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Named("store")
@Singleton
@Startup
public class Store {

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Queue<Todo> store =
            new ConcurrentLinkedDeque<>();

    @Interceptors(StoreStateInterceptor.class)
    public Todo save(Todo data) {
        Todo todo = new Todo(idGenerator.getAndIncrement(), data.getTitle(),
                data.getPriority(), data.isCompleted());
        store.add(todo);


        return todo;
    }

    public Optional<Todo> get(final long id) {
        return store.stream()
                .filter(t -> t.getId() == id)
                .findFirst();
    }

    public List<Todo> getAll() {
        return store.stream()
                .sorted(Comparator.comparing(Todo::getPriority).reversed())
                .collect(Collectors.toList());
    }

    @Interceptors(StoreStateInterceptor.class)
    public Optional<Todo> save(long id, Todo data) {
        boolean removed = remove(id);

        if (removed) {
            return Optional.of(save(data));
        } else {
            return Optional.empty();
        }
    }

    @Interceptors(StoreStateInterceptor.class)
    public boolean remove(long id) {
        return store.removeIf(todo -> todo.getId() == id);
    }

    public List<Todo> getStore() {
        return new ArrayList<>(store);
    }
}

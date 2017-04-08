package com.example.ui.todo.data;

import com.example.ui.todo.Store;
import com.example.ui.todo.Todo;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class StoreTest {

    private Store store = new Store();

    @Before
    public void setup() {
        store = new Store();
    }

    @Test
    public void should_store_item() {
        Todo t = new Todo(0, "First", 1, false);

        Todo saved = store.save(t);
        assertThat(saved.getId(), equalTo(1l));
        assertThat(store.get(1).get(), equalTo(new Todo(1, "First", 1, false)));
    }

    @Test
    public void should_retrive_sorted() {
        Todo t1 = new Todo(0, "First", 1, false);
        Todo t2 = new Todo(0, "Second", 2, false);

        Todo saved_t2 = store.save(t2);
        Todo saved_t1 = store.save(t1);

        List<Todo> all = store.getAll();
        assertThat(all, hasSize(2));
        assertThat(all.get(0), equalTo(saved_t2));
    }

    @Test
    public void should_remove_item() {
        Todo t1 = new Todo(0, "First", 2, false);
        Todo t2 = new Todo(0, "Second", 1, false);

        store.save(t1); store.save(t2);

        assertThat(store.remove(2), equalTo(true));
        assertThat(store.remove(3), equalTo(false));
    }

    @Test
    public void should_update_item() {
        Todo t1 = new Todo(0, "First", 1, false);
        Todo t2 = new Todo(0, "Second", 1, false);

        Todo saved_t1 = store.save(t1);
        Optional<Todo> save_existing = store.save(saved_t1.getId(), t2);
        Optional<Todo> save_not_existing = store.save(123l, t2);

        assertThat(store.get(saved_t1.getId()).isPresent(), equalTo(false));
        assertThat(save_existing.isPresent(), equalTo(true));
        assertThat(save_not_existing.isPresent(), equalTo(false));
        assertThat(store.get(save_existing.get().getId()).isPresent(), equalTo(true));
    }
}
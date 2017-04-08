package com.example.ui.todo.jsf;

import com.example.ui.todo.Store;
import com.example.ui.todo.Todo;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import java.util.List;

@ManagedBean
@RequestScoped
public class TodoBean {

    @Inject
    Store store;

    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Todo> getAllTodos() {
        return store.getAll();
    }

    public void save(Todo t) {
        store.save(t.getId(), t);
    }

    public void remove(Todo t) {
        store.remove(t.getId());
    }

    public void save() {
        store.save(new Todo(0, title, -1, false));
        this.title = new String();
    }

}

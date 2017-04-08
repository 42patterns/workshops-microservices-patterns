package com.example.ui.todo;

import javax.xml.bind.annotation.XmlElement;

public class Todo {

	private long id;
	private String title;
	@XmlElement(name = "order")
	private long priority;
	private boolean completed;

	public Todo() {
	}

	public Todo(long id, String title, long priority, boolean completed) {
		this.id = id;
		this.title = title;
		this.priority = priority;
		this.completed = completed;
	}

	public long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public long getPriority() {
		return priority;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setPriority(long priority) {
		this.priority = priority;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Todo todo = (Todo) o;

		if (id != todo.id) return false;
		if (priority != todo.priority) return false;
		if (completed != todo.completed) return false;
		return title != null ? title.equals(todo.title) : todo.title == null;
	}

    @Override
    public String toString() {
        return "Todo{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", priority=" + priority +
                ", completed=" + completed +
                '}';
    }
}

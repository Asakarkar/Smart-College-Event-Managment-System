package com.college.event.patterns.proxy;

import com.college.event.database.DatabaseManager;
import com.college.event.model.Registration;
import com.college.event.model.Student;
import com.college.event.patterns.singleton.EventManagementSystem;

public class RealEventSystem implements EventSystem {
    private final Student student;
    private final DatabaseManager db;

    public RealEventSystem(Student student) {
        this.student = student;
        this.db = EventManagementSystem.getInstance().getDatabase();
    }

    public Student login(String username, String password) {
        return student;
    }

    public Registration register(int eventId) {
        return EventManagementSystem.getInstance().register(student.getId(), eventId);
    }
}
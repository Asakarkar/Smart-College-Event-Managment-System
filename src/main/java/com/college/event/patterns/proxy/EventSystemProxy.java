package com.college.event.patterns.proxy;

import com.college.event.database.DatabaseManager;
import com.college.event.model.Registration;
import com.college.event.model.Student;

public class EventSystemProxy implements EventSystem {
    private final DatabaseManager db;
    private RealEventSystem realSystem;
    private Student authenticatedStudent;

    public EventSystemProxy(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Student login(String username, String password) {
        authenticatedStudent = db.authenticate(username, password);
        if (authenticatedStudent != null) {
            realSystem = new RealEventSystem(authenticatedStudent);
        }
        return authenticatedStudent;
    }

    @Override
    public Registration register(int eventId) {
        if (realSystem == null) {
            throw new SecurityException("Access denied. Please login first.");
        }
        return realSystem.register(eventId);
    }
}
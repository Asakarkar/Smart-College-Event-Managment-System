package com.college.event.patterns.singleton;

import com.college.event.database.DatabaseManager;
import com.college.event.model.Registration;

public class EventManagementSystem {
    private static final EventManagementSystem INSTANCE = new EventManagementSystem();
    private final DatabaseManager database;

    private EventManagementSystem() {
        database = new DatabaseManager();
    }

    public static EventManagementSystem getInstance() {
        return INSTANCE;
    }

    public DatabaseManager getDatabase() {
        return database;
    }

    public Registration register(int studentId, int eventId) {
        return database.createRegistration(studentId, eventId);
    }
}
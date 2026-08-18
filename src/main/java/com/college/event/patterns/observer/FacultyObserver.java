package com.college.event.patterns.observer;

public class FacultyObserver implements EventObserver {
    public void update(int registrationId, String eventName, String status) {
        System.out.println("Faculty Coordinator notified: Registration " +
                registrationId + " -> " + status);
    }
}
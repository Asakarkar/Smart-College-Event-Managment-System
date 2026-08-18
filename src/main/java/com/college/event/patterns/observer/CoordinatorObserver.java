package com.college.event.patterns.observer;

public class CoordinatorObserver implements EventObserver {
    public void update(int registrationId, String eventName, String status) {
        System.out.println("Event Coordinator updated: Registration " +
                registrationId + " -> " + status);
    }
}
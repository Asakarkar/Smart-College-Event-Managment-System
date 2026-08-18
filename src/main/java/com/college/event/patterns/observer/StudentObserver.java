package com.college.event.patterns.observer;

public class StudentObserver implements EventObserver {
    public void update(int registrationId, String eventName, String status) {
        System.out.println("Student notified: Registration " + registrationId +
                " for " + eventName + " is " + status + ".");
    }
}
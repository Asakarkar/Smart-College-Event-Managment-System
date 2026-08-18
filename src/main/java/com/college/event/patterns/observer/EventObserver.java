package com.college.event.patterns.observer;

public interface EventObserver {
    void update(int registrationId, String eventName, String status);
}
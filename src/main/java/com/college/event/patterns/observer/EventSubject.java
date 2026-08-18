package com.college.event.patterns.observer;

public interface EventSubject {
    void registerObserver(EventObserver observer);
    void removeObserver(EventObserver observer);
    void notifyObservers(int registrationId, String eventName, String status);
}
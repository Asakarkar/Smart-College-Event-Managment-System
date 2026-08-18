package com.college.event.patterns.observer;

import java.util.ArrayList;
import java.util.List;

public class EventCoordinatorNotifier implements EventSubject {
    private final List<EventObserver> observers = new ArrayList<>();

    public void registerObserver(EventObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(EventObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(int registrationId, String eventName, String status) {
        for (EventObserver observer : observers) {
            observer.update(registrationId, eventName, status);
        }
    }
}
package com.college.event.patterns.factory;

import com.college.event.model.BaseEvent;
import com.college.event.model.Event;

public class TechnicalEventFactory implements EventFactory {
    public Event createEvent(String name, String description, String date,
                             String venue, int capacity) {
        return new BaseEvent(name, "TECHNICAL", description, date, venue, capacity);
    }
}
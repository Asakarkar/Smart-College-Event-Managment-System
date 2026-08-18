package com.college.event.patterns.factory;

import com.college.event.model.Event;

public interface EventFactory {
    Event createEvent(String name, String description, String date,
                      String venue, int capacity);
}
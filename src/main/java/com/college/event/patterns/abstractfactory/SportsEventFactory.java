package com.college.event.patterns.abstractfactory;

import com.college.event.model.BaseEvent;
import com.college.event.model.Event;

public class SportsEventFactory implements EventFamilyFactory {
    public Event createEvent(String name, String description, String date,
                             String venue, int capacity) {
        return new BaseEvent(name, "SPORTS", description, date, venue, capacity);
    }
    public String createRegistrationLabel() { return "Sports Registration"; }
    public String createCertificateLabel() { return "Sports Certificate"; }
}
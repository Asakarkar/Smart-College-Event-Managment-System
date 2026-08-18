package com.college.event.patterns.abstractfactory;

import com.college.event.model.BaseEvent;
import com.college.event.model.Event;

public class TechnicalEventFactory implements EventFamilyFactory {
    public Event createEvent(String name, String description, String date,
                             String venue, int capacity) {
        return new BaseEvent(name, "TECHNICAL", description, date, venue, capacity);
    }
    public String createRegistrationLabel() { return "Technical Registration"; }
    public String createCertificateLabel() { return "Technical Certificate"; }
}
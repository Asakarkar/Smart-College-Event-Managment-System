package com.college.event.patterns.abstractfactory;

import com.college.event.model.Event;

public interface EventFamilyFactory {
    Event createEvent(String name, String description, String date,
                      String venue, int capacity);
    String createRegistrationLabel();
    String createCertificateLabel();
}
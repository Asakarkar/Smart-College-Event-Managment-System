package com.college.event.model;

public interface Event {
    default int getId() { return 0; }
    String getName();
    String getCategory();
    String getDescription();
    String getDate();
    String getVenue();
    int getCapacity();
    default String getStatus() { return "OPEN"; }
    default String getType() { return getCategory(); }
}